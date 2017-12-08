package cool.graph.client.mutactions

import cool.graph.DataItem
import cool.graph.Types.UserData
import cool.graph.client.ClientInjector
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database._
import cool.graph.cuid.Cuid
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.database.Databases
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
  case class ImportNodeValue(index: Int, identifier: ImportIdentifier, values: Map[String, Any])
  case class ImportRelation(index: Int, relationName: String, left: ImportRelationSide, right: ImportRelationSide)
  case class ImportListValue(index: Int, identifier: ImportIdentifier, values: Map[String, Vector[Any]])

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
    implicit val importNodeValue: RootJsonFormat[ImportNodeValue]       = jsonFormat3(ImportNodeValue)
    implicit val importListValue: RootJsonFormat[ImportListValue]       = jsonFormat3(ImportListValue)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat4(ImportRelation)
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

  //generate Indices at the end to simplify code?
  //hand down stringbuilder with max capacity already?

  //{"type": "nodes", "cursor": {"table"}}

  sealed trait ExportInfo { val length: Int; val index: Int; val hasNext: Boolean }
  case class NodeInfo(dataResolver: DataResolver, models: List[(Model, Int)], index: Int) extends ExportInfo {
    val length: Int      = models.length
    val current: Model   = models.find(_._2 == index).get._1
    val hasNext: Boolean = index < length - 1
    def nextModel: Model = models.find(_._2 == index + 1).get._1

  }
  case class ListInfo(dataResolver: DataResolver,
                      models: List[(Model, Int)],
                      index: Int,
                      nodeIndex: Int,
                      fieldIndex: Int,
                      listFields: List[(String, Int)],
                      arrayIndex: Int)
      extends ExportInfo {
    val length: Int           = models.length
    val fieldLength: Int      = listFields.length
    val currentModel: Model   = models.find(_._2 == index).get._1
    val currentField: String  = listFields.find(_._2 == index).get._1
    val hasNext: Boolean      = index < length - 1
    val hasNextField: Boolean = fieldIndex < fieldLength - 1
    def nextModel: Model      = models.find(_._2 == index + 1).get._1
    def nextField: String     = listFields.find(_._2 == index + 1).get._1
  }
  case class RelationInfo(dataResolver: DataResolver, relations: List[(RelationData, Int)], index: Int) extends ExportInfo {
    val length: Int                = relations.length
    val current: RelationData      = relations.find(_._2 == index).get._1
    val hasNext: Boolean           = index < length - 1
    def nextRelation: RelationData = relations.find(_._2 == index + 1).get._1
  }

  def isLimitReached(str: String): Boolean = str.length > 10000000 // only for testing purposes variable in here

  def exportNodes(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int): Future[StringWithCursor] = {
    resultForCursor(in = "", startRow, project, NodeInfo(dataResolver, project.models.zipWithIndex, tableIndex))
  }

  def exportRelations(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int): Future[StringWithCursor] = {
    val relationInfos = project.relations.map(r => generateRelationData(r, project))

    resultForCursor(in = "", startRow, project, RelationInfo(dataResolver, relationInfos.zipWithIndex, tableIndex))
  }

  //this needs to return a cursor with way more information -.- model, node, field, position in field
  def exportLists(project: Project, dataResolver: DataResolver, tableIndex: Int, startRow: Int, fieldIndex: Int, arrayIndex: Int): Future[StringWithCursor] = {
    val tablesWithIndex: List[(Model, Int)]  = project.models.filter(m => m.fields.exists(f => f.isList)).zipWithIndex
    val currentModel: Model                  = tablesWithIndex.find(_._2 == tableIndex).get._1
    val fieldsWithIndex: List[(String, Int)] = currentModel.scalarListFields.map(_.name).zipWithIndex

    resultForCursor(in = "", startRow, project, ListInfo(dataResolver, tablesWithIndex, tableIndex, startRow, fieldIndex, fieldsWithIndex, arrayIndex))
  }

  def resultForCursor(in: String, startRow: Int, project: Project, info: ExportInfo, fileIndex: Int = 0): Future[StringWithCursor] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def generateNextInfo: ExportInfo = info match { // dumb trait can't just be copied -.-
      case info: NodeInfo     => info.copy(index = info.index + 1)
      case info: ListInfo     => info.copy(index = info.index + 1)
      case info: RelationInfo => info.copy(index = info.index + 1)
    }

    for {
      tableResult <- resultForTable(in, fileIndex, startRow, info)
      x <- tableResult.isFull match {
            case false if info.hasNext  => resultForCursor(tableResult.out, startRow = 0, project, generateNextInfo, tableResult.nextFileIndex)
            case false if !info.hasNext => Future.successful(StringWithCursor(tableResult.out, -1, -1))
            case true                   => Future.successful(StringWithCursor(tableResult.out, info.index, tableResult.endRow))
          }
    } yield x
  }

  case class TableResult(out: String, nextFileIndex: Int, endRow: Int, isFull: Boolean)
  def resultForTable(in: String, fileIndex: Int, startRow: Int, info: ExportInfo): Future[TableResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    fetchDataItemsPage(startRow, info).flatMap { page =>
      val pageResult = serializePage(in, page, fileIndex, info)

      (pageResult.isFull, page.hasMore) match {
        case (false, true)  => resultForTable(in = pageResult.out, pageResult.nextFileIndex, startRow + 1000, info)
        case (false, false) => Future.successful(TableResult(pageResult.out, pageResult.nextFileIndex, endRow = -1, false))
        case (true, _)      => Future.successful(TableResult(pageResult.out, pageResult.nextFileIndex, startRow + pageResult.used, true))
      }
    }
  }

  case class DataItemsPage(items: Seq[DataItem], hasMore: Boolean) { def itemCount: Int = items.length }
  def fetchDataItemsPage(startOnModel: Int, info: ExportInfo): Future[DataItemsPage] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val queryArguments = QueryArguments(skip = Some(startOnModel), after = None, first = Some(1000), None, None, None, None)
    for {
      resolverResult <- info match {
                         case x: NodeInfo     => x.dataResolver.loadModelRowsForExport(x.current, Some(queryArguments))
                         case x: ListInfo     => x.dataResolver.loadModelRowsForExport(x.currentModel, Some(queryArguments)) //own select only for list fields?
                         case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
                       }
    } yield {
      DataItemsPage(resolverResult.items, hasMore = resolverResult.hasNextPage)
    }
  }

  case class PageResult(out: String, nextFileIndex: Int, used: Int, isFull: Boolean)
  def serializePage(in: String, page: DataItemsPage, fileIndex: Int, info: ExportInfo, startOnPage: Int = 0, amount: Int = 1000): PageResult = {

    val dataItems           = page.items.slice(startOnPage, startOnPage + amount)
    val serializationResult = serializeDataItems(in, dataItems, fileIndex, info)
    val combinedString      = if (in.nonEmpty) in + "," + serializationResult.out else serializationResult.out
    val numberSerialized    = dataItems.length
    val isLimitReachedTemp  = isLimitReached(combinedString)
    val noneLeft            = startOnPage + amount >= page.itemCount

    isLimitReachedTemp match {
      case true if amount == 1 => PageResult(out = in, nextFileIndex = fileIndex, used = startOnPage, isFull = true)
      case false if noneLeft   => PageResult(out = combinedString, nextFileIndex = fileIndex + numberSerialized, startOnPage + numberSerialized + 1, false)
      case true                => serializePage(in = in, page = page, fileIndex = fileIndex, info, startOnPage = startOnPage, amount / 10)
      case false               => serializePage(in = combinedString, page, fileIndex + numberSerialized, info, startOnPage + numberSerialized, amount)
    }
  }

  case class SerializeResult(out: String, nextFileIndex: Int)
  def serializeDataItems(in: String, dataItems: Seq[DataItem], startIndex: Int, info: ExportInfo): SerializeResult = {
    var index = startIndex
    val serializedItems: Seq[String] = for {
      item <- dataItems
    } yield {
      val tmp: String = info match {
        case info: NodeInfo     => dataItemToExportNode(index, item, info)
        case info: ListInfo     => dataItemToExportList(in, index, item, info)
        case info: RelationInfo => dataItemToExportRelation(index, item, info)
      }
      index = index + 1
      tmp
    }
    SerializeResult(out = serializedItems.mkString(","), nextFileIndex = index)
  }

  def dataItemToExportNode(index: Int, item: DataItem, info: NodeInfo): String = {
    import MyJsonProtocol._
    import spray.json._

    val dataValueMap: UserData                          = item.userData
    val createdAtUpdatedAtMap                           = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
    val withoutImplicitFields: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
    val nonListFieldsWithValues: Map[String, Any]       = withoutImplicitFields.collect { case (k, Some(v)) if !info.current.getFieldByName_!(k).isList => (k, v) }
    val outputMap: Map[String, Any]                     = nonListFieldsWithValues ++ createdAtUpdatedAtMap
    val importIdentifier: ImportIdentifier              = ImportIdentifier(info.current.name, item.id)
    val nodeValue: ImportNodeValue                      = ImportNodeValue(index = index, identifier = importIdentifier, values = outputMap)
    nodeValue.toJson.toString
  }

  // needs to be able to communicate back up the chain if and where it stopped also needs to return an index
  def dataItemToExportList(in: String, index: Int, item: DataItem, info: ListInfo): String = {
    val listFieldsWithValues: Map[String, Any] = item.userData.collect { case (k, Some(v)) if info.listFields.map(p => p._1).contains(k) => (k, v) }
    val importIdentifier: ImportIdentifier     = ImportIdentifier(info.currentModel.name, item.id)
    val nodeResults                            = serializeFields(in, index, importIdentifier, listFieldsWithValues, info)
    nodeResults.out
  }

  case class FieldResult(out: String, nextFileIndex: Int, usedFields: Int, usedArrayItems: Int, isFull: Boolean)
  def serializeFields(in: String, fileIndex: Int, identifier: ImportIdentifier, nodeValues: Map[String, Any], info: ListInfo): Result = {
    import spray.json

    val arrayValues = Vector("f", "d", "h") //nodeValues(info.currentField).parseJson
    val result      = serializeArray(in, fileIndex, identifier, arrayValues, info)

    result.isFull match {
      case false if info.hasNextField =>
        serializeFields(result.out, result.nextFileIndex, identifier, nodeValues, info.copy(fieldIndex = info.fieldIndex + 1, arrayIndex = 0))
      case false => result
      case true  => result

    }
  }

  case class Result(out: String, nextFileIndex: Int, modelIndex: Int, nodeIndex: Int, fieldIndex: Int, arrayIndex: Int, isFull: Boolean)

  //this should have the ability to scale up again, but probably not within one field
  def serializeArray(in: String, fileIndex: Int, identifier: ImportIdentifier, arrayValues: Vector[Any], info: ListInfo, amount: Int = 1000000): Result = {
    import MyJsonProtocol._
    import spray.json._

    val values                     = arrayValues.slice(info.arrayIndex, info.arrayIndex + amount)
    val nodeValue: ImportListValue = ImportListValue(index = fileIndex, identifier = identifier, values = Map(info.currentField -> values))
    val string                     = nodeValue.toJson.toString
    val combinedString             = if (in.nonEmpty) in + "," + string else string
    val numberSerialized           = values.length
    val noneLeft                   = info.arrayIndex + amount >= arrayValues.length

    isLimitReached(combinedString) match {
      case true if amount == 1 => Result(in, fileIndex, info.index, info.nodeIndex, info.fieldIndex, info.arrayIndex, isFull = true)
      case false if noneLeft   => Result(combinedString, fileIndex + 1, info.index, info.nodeIndex, info.fieldIndex, -5, isFull = false)
      case false               => serializeArray(combinedString, fileIndex + 1, identifier, arrayValues, info.copy(arrayIndex = info.arrayIndex + numberSerialized), amount)
      case true                => serializeArray(in, fileIndex, identifier, arrayValues, info, amount / 10)
    }

  } //use info classes as in and output? and then use .copy would probably be good to have the custom .copy then

  case class RelationData(relationId: String, relationName: String, leftModel: String, leftField: String, rightModel: String, rightField: String)
  def generateRelationData(r: Relation, project: Project): RelationData = {
    RelationData(r.id, r.name, r.getModelB_!(project).name, r.getModelBField_!(project).name, r.getModelA_!(project).name, r.getModelAField_!(project).name)
  }

  //this seems to switch relationfields around on self relations
  def dataItemToExportRelation(index: Int, item: DataItem, info: RelationInfo): String = {
    import MyJsonProtocol._
    import spray.json._
    val idA               = item.userData("A").get.toString
    val idB               = item.userData("B").get.toString
    val leftIdentifier    = ImportIdentifier(info.current.leftModel, idB)
    val leftRelationSide  = ImportRelationSide(leftIdentifier, info.current.leftField)
    val rightIdentifier   = ImportIdentifier(info.current.rightModel, idA)
    val rightRelationSide = ImportRelationSide(rightIdentifier, info.current.rightField)
    val exportRelation    = ImportRelation(index, info.current.relationName, leftRelationSide, rightRelationSide)
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
