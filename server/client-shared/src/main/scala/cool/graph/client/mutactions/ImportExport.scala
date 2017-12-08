package cool.graph.client.mutactions

import cool.graph.DataItem
import cool.graph.Types.UserData
import cool.graph.client.ClientInjector
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database._
import cool.graph.cuid.Cuid
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.database.Databases
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.Future
import scala.util.Try

object ImportExportFormat {
  case class StringWithCursor(string: String, modelIndex: Int, nodeRow: Int)

  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: String)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: String)
  case class ImportNodeValue(identifier: ImportIdentifier, values: Map[String, Any])
  case class ImportRelation(relationName: String, left: ImportRelationSide, right: ImportRelationSide)
  case class ImportListValue(identifier: ImportIdentifier, values: Map[String, Vector[Any]])

  object MyJsonProtocol extends DefaultJsonProtocol {

    //from requestpipelinerunner -> there's 10 different versions of this all over the place -.-
    implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any): JsValue = x match {
        case m: Map[_, _]   => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
        case l: List[Any]   => JsArray(l.map(write).toVector)
        case l: Vector[Any] => JsArray(l.map(write))
        case l: Seq[Any]    => JsArray(l.map(write).toVector)
        case n: Int         => JsNumber(n)
        case n: Long        => JsNumber(n)
        case n: BigDecimal  => JsNumber(n)
        case n: Double      => JsNumber(n)
        case s: String      => JsString(s)
        case true           => JsTrue
        case false          => JsFalse
        case v: JsValue     => v
        case null           => JsNull
        case r              => JsString(r.toString)
      }

      def read(x: JsValue): Any = {
        x match {
          case l: JsArray   => l.elements.map(read).toList
          case m: JsObject  => m.fields.mapValues(read)
          case s: JsString  => s.value
          case n: JsNumber  => n.value
          case b: JsBoolean => b.value
          case JsNull       => null
          case _            => sys.error("implement all scalar types!")
        }
      }
    }

    implicit val importBundle: RootJsonFormat[ImportBundle]             = jsonFormat2(ImportBundle)
    implicit val importIdentifier: RootJsonFormat[ImportIdentifier]     = jsonFormat2(ImportIdentifier)
    implicit val importRelationSide: RootJsonFormat[ImportRelationSide] = jsonFormat2(ImportRelationSide)
    implicit val importNodeValue: RootJsonFormat[ImportNodeValue]       = jsonFormat2(ImportNodeValue)
    implicit val importListValue: RootJsonFormat[ImportListValue]       = jsonFormat2(ImportListValue)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat3(ImportRelation)
  }
}

object DataImport {
  import cool.graph.client.mutactions.ImportExportFormat._

  def executeGeneric(project: Project, json: JsValue)(implicit injector: ClientInjector): Future[Vector[String]] = {
    import MyJsonProtocol._

    import scala.concurrent.ExecutionContext.Implicits.global
    val bundle = json.convertTo[ImportBundle]
    val cnt    = bundle.values.elements.length

    val actions = bundle.valueType match {
      case "nodes"      => generateImportNodesDBActions(project, bundle.values.elements.map(_.convertTo[ImportNodeValue]))
      case "relations"  => generateImportRelationsDBActions(project, bundle.values.elements.map(_.convertTo[ImportRelation]))
      case "listvalues" => generateImportListsDBActions(project, bundle.values.elements.map(_.convertTo[ImportListValue]))
    }

    val res: Future[Vector[Try[Int]]]                       = runDBActions(project, actions)
    def messageWithOutConnection(tryelem: Try[Any]): String = tryelem.failed.get.getMessage.substring(tryelem.failed.get.getMessage.indexOf(")") + 1)

    res.map(vector =>
      vector.zipWithIndex.collect {
        case (elem, idx) if elem.isFailure && idx < cnt  => s"Index: $idx Message: ${messageWithOutConnection(elem)}"
        case (elem, idx) if elem.isFailure && idx >= cnt => s"Index: ${idx - cnt} Message: Relay Id Failure ${messageWithOutConnection(elem)}"
    })
  }

  def generateImportNodesDBActions(project: Project, nodes: Vector[ImportNodeValue]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
    val items = nodes.map { element =>
      val id                              = element.identifier.id
      val model                           = project.getModelByName_!(element.identifier.typeName)
      val listFields: Map[String, String] = model.scalarFields.filter(_.isList).map(field => field.name -> "[]").toMap
      val values: Map[String, Any]        = element.values ++ listFields + ("id" -> id)
      DatabaseMutationBuilder.createDataItem(project.id, model.name, values).asTry
    }
    val relayIds: TableQuery[ProjectRelayIdTable] = TableQuery(new ProjectRelayIdTable(_, project.id))
    val relay = nodes.map { element =>
      val id    = element.identifier.id
      val model = project.getModelByName_!(element.identifier.typeName)
      val x     = relayIds += ProjectRelayId(id = id, model.id)
      x.asTry
    }
    DBIO.sequence(items ++ relay)
  }

  def generateImportRelationsDBActions(project: Project, relations: Vector[ImportRelation]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
    val x = relations.map { element =>
      val fromModel                                                 = project.getModelByName_!(element.left.identifier.typeName)
      val fromField                                                 = fromModel.getFieldByName_!(element.left.fieldName)
      val relationSide: cool.graph.shared.models.RelationSide.Value = fromField.relationSide.get
      val relation: Relation                                        = fromField.relation.get

      val aValue: String = if (relationSide == RelationSide.A) element.left.identifier.id else element.right.identifier.id
      val bValue: String = if (relationSide == RelationSide.A) element.right.identifier.id else element.left.identifier.id

      val aModel: Model = relation.getModelA_!(project)
      val bModel: Model = relation.getModelB_!(project)

      def getFieldMirrors(model: Model, id: String) =
        relation.fieldMirrors
          .filter(mirror => model.fields.map(_.id).contains(mirror.fieldId))
          .map(mirror => {
            val field = project.getFieldById_!(mirror.fieldId)
            MirrorFieldDbValues(
              relationColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation),
              modelColumnName = field.name,
              model.name,
              id
            )
          })

      val fieldMirrors: List[MirrorFieldDbValues] = getFieldMirrors(aModel, aValue) ++ getFieldMirrors(bModel, bValue)

      DatabaseMutationBuilder.createRelationRow(project.id, relation.id, Cuid.createCuid(), aValue, bValue, fieldMirrors).asTry
    }
    DBIO.sequence(x)
  }

  def generateImportListsDBActions(project: Project, lists: Vector[ImportListValue]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
    val x = lists.map { element =>
      val id    = element.identifier.id
      val model = project.getModelByName_!(element.identifier.typeName)
      DatabaseMutationBuilder.updateDataItemListValue(project.id, model.name, id, element.values).asTry
    }
    DBIO.sequence(x)
  }

  def runDBActions(project: Project, actions: DBIOAction[Vector[Try[Int]], NoStream, Effect.Write])(
      implicit injector: ClientInjector): Future[Vector[Try[Int]]] = {
    val db: Databases = injector.globalDatabaseManager.getDbForProject(project)
    db.master.run(actions)
  }
}

object DataExport {
  import cool.graph.client.mutactions.ImportExportFormat._

  //hand down stringbuilder with max capacity already?
  //use info classes as in and output? and then use .copy would probably be good to have the custom .copy then
  //{"type": "nodes", "cursor": {"table": INT, "row": INT, "field": INT, "array": INT}}

  sealed trait ExportInfo { val cursor: Cursor; val hasNext: Boolean }
  case class NodeInfo(dataResolver: DataResolver, models: List[(Model, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int      = models.length
    val current: Model   = models.find(_._2 == cursor.table).get._1
    val hasNext: Boolean = cursor.table < length - 1
    def nextModel: Model = models.find(_._2 == cursor.table + 1).get._1

  }
  case class ListInfo(dataResolver: DataResolver, models: List[(Model, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int                                     = models.length
    val currentModel: Model                             = models.find(_._2 == cursor.table).get._1
    val listFields: List[(String, TypeIdentifier, Int)] = currentModel.scalarListFields.zipWithIndex.map { case (f, i) => (f.name, f.typeIdentifier, i) }
    val fieldLength: Int                                = listFields.length
    val currentField: String                            = listFields.find(_._3 == cursor.table).get._1
    val currentTypeIdentifier: TypeIdentifier           = listFields.find(_._3 == cursor.table).get._2
    val hasNext: Boolean                                = cursor.table < length - 1
    val hasNextField: Boolean                           = cursor.field < fieldLength - 1
    def nextModel: Model                                = models.find(_._2 == cursor.table + 1).get._1
    def nextField: String                               = listFields.find(_._3 == cursor.table + 1).get._1
  }
  case class RelationInfo(dataResolver: DataResolver, relations: List[(RelationData, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int                = relations.length
    val current: RelationData      = relations.find(_._2 == cursor.table).get._1
    val hasNext: Boolean           = cursor.table < length - 1
    def nextRelation: RelationData = relations.find(_._2 == cursor.table + 1).get._1
  }

  case class Cursor(table: Int, row: Int, field: Int, array: Int)
  case class Result(out: String, cursor: Cursor, isFull: Boolean)

  def isLimitReached(str: String): Boolean = str.length > 1000 // only for testing purposes variable in here

  def exportNodes(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int): Future[Result] = {
    resultForCursor(in = "", NodeInfo(dataResolver, project.models.zipWithIndex, Cursor(tableIndex, startRow, -5, -5)))
  }

  def exportRelations(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int): Future[Result] = {
    val relationInfos = project.relations.map(r => generateRelationData(r, project)).zipWithIndex
    resultForCursor(in = "", RelationInfo(dataResolver, relationInfos, Cursor(tableIndex, startRow, -5, -5)))
  }

  def exportLists(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int, fieldIndex: Int, arrayIndex: Int): Future[Result] = {
    val modelsWithListFields: List[(Model, Int)] = project.models.filter(m => m.fields.exists(f => f.isList)).zipWithIndex
    resultForCursor(in = "", ListInfo(dataResolver, modelsWithListFields, Cursor(tableIndex, startRow, fieldIndex, arrayIndex)))
  }

  def resultForCursor(in: String, info: ExportInfo): Future[Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def updateCursor: ExportInfo = info match { // dumb trait can't just be copied -.-
      case info: NodeInfo     => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
      case info: ListInfo     => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
      case info: RelationInfo => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
    }

    for {
      result <- resultForTable(in, info)
      x <- result.isFull match {
            case false if info.hasNext  => resultForCursor(result.out, updateCursor)
            case false if !info.hasNext => Future.successful(result.copy(cursor = Cursor(-1, -1, -1, -1)))
            case true                   => Future.successful(result)
          }
    } yield x
  }

  def resultForTable(in: String, info: ExportInfo): Future[Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def generateNextInfo: ExportInfo = info match { // dumb trait can't just be copied -.-
      case info: NodeInfo     => info.copy(cursor = info.cursor.copy(row = info.cursor.row + 1000))
      case info: ListInfo     => info.copy(cursor = info.cursor.copy(row = info.cursor.row + 1000))
      case info: RelationInfo => info.copy(cursor = info.cursor.copy(row = info.cursor.row + 1000))
    }

    fetchDataItemsPage(info).flatMap { page =>
      val result = serializePage(in, page, info)

      (result.isFull, page.hasMore) match {
        case (false, true)  => resultForTable(in = result.out, generateNextInfo)
        case (false, false) => Future.successful(result)
        case (true, _)      => Future.successful(result)
      }
    }
  }

  case class DataItemsPage(items: Seq[DataItem], hasMore: Boolean) { def itemCount: Int = items.length }
  def fetchDataItemsPage(info: ExportInfo): Future[DataItemsPage] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val queryArguments = QueryArguments(skip = Some(info.cursor.row), after = None, first = Some(1000), None, None, None, None)
    for {
      result <- info match {
                 case x: NodeInfo     => x.dataResolver.loadModelRowsForExport(x.current, Some(queryArguments))
                 case x: ListInfo     => x.dataResolver.loadModelRowsForExport(x.currentModel, Some(queryArguments)) //own select only for list fields?
                 case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
               }
    } yield {
      DataItemsPage(result.items, hasMore = result.hasNextPage)
    }
  }

  def serializePage(in: String, page: DataItemsPage, info: ExportInfo, startOnPage: Int = 0, amount: Int = 1000): Result = {

    val dataItems = page.items.slice(startOnPage, startOnPage + amount)
    val result    = serializeDataItems(in, dataItems, info)
//    val combinedString      = if (in.nonEmpty) in + "," + result.out else result.out
//    val numberSerialized    = dataItems.length
//    val isLimitReachedTemp  = isLimitReached(combinedString)
    val noneLeft = startOnPage + amount >= page.itemCount

    result.isFull match {
      case true if amount == 1 => result
      case false if noneLeft   => result
      case true                => serializePage(in = in, page = page, info, startOnPage, amount / 10)
      case false               => serializePage(in = result.out, page, info, result.cursor.row, amount)
    }
  }

  def serializeDataItems(in: String, dataItems: Seq[DataItem], info: ExportInfo): Result = {

    info match {
      case info: NodeInfo =>
        val string           = dataItems.map(item => dataItemToExportNode(item, info)).mkString(",")
        val combinedString   = if (in.nonEmpty) in + "," + string else string
        val numberSerialized = dataItems.length

        isLimitReached(combinedString) match {
          case true  => Result(in, info.cursor, isFull = true)
          case false => Result(combinedString, info.cursor.copy(row = info.cursor.row + numberSerialized), isFull = false)
        }

      case info: RelationInfo =>
        val string           = dataItems.map(item => dataItemToExportRelation(item, info)).mkString(",")
        val combinedString   = if (in.nonEmpty) in + "," + string else string
        val numberSerialized = dataItems.length

        isLimitReached(combinedString) match {
          case true  => Result(in, info.cursor, isFull = true)
          case false => Result(combinedString, info.cursor.copy(row = info.cursor.row + numberSerialized), isFull = false)
        }
      case info: ListInfo =>
        // this is only for list values
//        val results: Seq[Result] = for {
//          item <- dataItems
//        } yield {
//          info match {
//            case info: NodeInfo     => dataItemToExportNode(item, info)
//            case info: ListInfo     => dataItemToExportList(in, item, info)
//            case info: RelationInfo => dataItemToExportRelation(item, info)
//          }
//
//        }
        Result("", Cursor(0, 0, 0, 0), true)
    }
  }

  def dataItemToExportNode(item: DataItem, info: NodeInfo): String = {
    import MyJsonProtocol._
    import spray.json._

    val dataValueMap: UserData                          = item.userData
    val createdAtUpdatedAtMap                           = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
    val withoutImplicitFields: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
    val nonListFieldsWithValues: Map[String, Any]       = withoutImplicitFields.collect { case (k, Some(v)) if !info.current.getFieldByName_!(k).isList => (k, v) }
    val outputMap: Map[String, Any]                     = nonListFieldsWithValues ++ createdAtUpdatedAtMap
    val importIdentifier: ImportIdentifier              = ImportIdentifier(info.current.name, item.id)
    val nodeValue: ImportNodeValue                      = ImportNodeValue(identifier = importIdentifier, values = outputMap)
    nodeValue.toJson.toString
  }

  // needs to be able to communicate back up the chain if and where it stopped also needs to return an index
  def dataItemToExportList(in: String, item: DataItem, info: ListInfo): Result = {
    import cool.graph.shared.schema.CustomScalarTypes.parseValueFromString
    val listFieldsWithValues: Map[String, Any] = item.userData.collect { case (k, Some(v)) if info.listFields.map(p => p._1).contains(k) => (k, v) }

    val convertedListFieldsWithValues = listFieldsWithValues.map {
      case (k, v) =>
        val any = parseValueFromString(v.toString, info.currentTypeIdentifier, true)
        println(any)

        val vector = any match {
          case Some(Some(x)) => x.asInstanceOf[Vector[Any]]
          case _             => Vector.empty
        }

        (k, vector)
    }

    val importIdentifier: ImportIdentifier = ImportIdentifier(info.currentModel.name, item.id)
    val nodeResults                        = serializeFields(in, importIdentifier, convertedListFieldsWithValues, info)
    nodeResults
  }

  def serializeFields(in: String, identifier: ImportIdentifier, fieldValues: Map[String, Vector[Any]], info: ListInfo): Result = {
    val result = serializeArray(in, identifier, fieldValues(info.currentField), info)

    result.isFull match {
      case false if info.hasNextField =>
        serializeFields(result.out, identifier, fieldValues, info.copy(cursor = info.cursor.copy(field = info.cursor.field + 1, array = 0)))
      case false => result
      case true  => result
    }
  }

  //this should have the ability to scale up again, but doing it within one field probably adds too much complexity for now
  def serializeArray(in: String, identifier: ImportIdentifier, arrayValues: Vector[Any], info: ListInfo, amount: Int = 1000000): Result = {
    import MyJsonProtocol._
    import spray.json._

    val values                     = arrayValues.slice(info.cursor.array, info.cursor.array + amount)
    val nodeValue: ImportListValue = ImportListValue(identifier = identifier, values = Map(info.currentField -> values))
    val string                     = nodeValue.toJson.toString
    val combinedString             = if (in.nonEmpty) in + "," + string else string
    val numberSerialized           = values.length
    val noneLeft                   = info.cursor.array + amount >= arrayValues.length

    isLimitReached(combinedString) match {
      case true if amount == 1 => Result(in, info.cursor, isFull = true)
      case false if noneLeft   => Result(combinedString, info.cursor.copy(array = -5), isFull = false)
      case false =>
        serializeArray(combinedString, identifier, arrayValues, info.copy(cursor = info.cursor.copy(array = info.cursor.array + numberSerialized)), amount)
      case true => serializeArray(in, identifier, arrayValues, info, amount / 10)
    }

  }

  case class RelationData(relationId: String, relationName: String, leftModel: String, leftField: String, rightModel: String, rightField: String)
  def generateRelationData(r: Relation, project: Project): RelationData = {
    RelationData(r.id, r.name, r.getModelB_!(project).name, r.getModelBField_!(project).name, r.getModelA_!(project).name, r.getModelAField_!(project).name)
  }

  //this seems to switch relationfields around on self relations on the same node
  def dataItemToExportRelation(item: DataItem, info: RelationInfo): String = {
    import MyJsonProtocol._
    import spray.json._
    val idA               = item.userData("A").get.toString
    val idB               = item.userData("B").get.toString
    val leftIdentifier    = ImportIdentifier(info.current.leftModel, idB)
    val leftRelationSide  = ImportRelationSide(leftIdentifier, info.current.leftField)
    val rightIdentifier   = ImportIdentifier(info.current.rightModel, idA)
    val rightRelationSide = ImportRelationSide(rightIdentifier, info.current.rightField)
    val exportRelation    = ImportRelation(info.current.relationName, leftRelationSide, rightRelationSide)
    exportRelation.toJson.toString
  }
}

object teststuff {

  def readFile(fileName: String): JsValue = {
    import spray.json._
    val json_string = scala.io.Source
      .fromFile(s"/Users/matthias/repos/github.com/graphcool/closed-source/integration-testing/src/test/scala/cool/graph/importData/$fileName")
      .getLines
      .mkString
    json_string.parseJson
  }
}
