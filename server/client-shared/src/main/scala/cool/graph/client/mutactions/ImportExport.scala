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

  case class ExportRequest(fileType: String, cursor: Cursor)      //{"fileType":"nodes","cursor":{"table":INT,"row":INT,"field":INT,"array":INT}}
  case class Cursor(table: Int, row: Int, field: Int, array: Int) //{"table":INT,"row":INT,"field":INT,"array":INT}
  case class ResultFormat(out: JsonBundle, cursor: Cursor, isFull: Boolean)
  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: String)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: String)
  case class ImportNode(identifier: ImportIdentifier, values: Map[String, Any])
  case class ImportRelation(left: ImportRelationSide, right: ImportRelationSide)
  case class ImportList(identifier: ImportIdentifier, values: Map[String, Vector[Any]])
  case class JsonBundle(jsonElements: Vector[JsValue], size: Int)

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

    implicit val jsonBundle: RootJsonFormat[JsonBundle]                 = jsonFormat2(JsonBundle)
    implicit val importBundle: RootJsonFormat[ImportBundle]             = jsonFormat2(ImportBundle)
    implicit val importIdentifier: RootJsonFormat[ImportIdentifier]     = jsonFormat2(ImportIdentifier)
    implicit val importRelationSide: RootJsonFormat[ImportRelationSide] = jsonFormat2(ImportRelationSide)
    implicit val importNodeValue: RootJsonFormat[ImportNode]            = jsonFormat2(ImportNode)
    implicit val importListValue: RootJsonFormat[ImportList]            = jsonFormat2(ImportList)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat2(ImportRelation)
    implicit val cursor: RootJsonFormat[Cursor]                         = jsonFormat4(Cursor)
    implicit val exportRequest: RootJsonFormat[ExportRequest]           = jsonFormat2(ExportRequest)
    implicit val resultFormat: RootJsonFormat[ResultFormat]             = jsonFormat3(ResultFormat)
  }
}

object DataImport {
  import cool.graph.client.mutactions.ImportExportFormat._

  def convertToImportNode(json: JsValue): ImportNode = {
    import MyJsonProtocol._
    val map              = json.convertTo[Map[String, Any]]
    val typeName: String = map("_typeName").asInstanceOf[String]
    val id: String       = map("id").asInstanceOf[String]
    val valueMap         = map.collect { case (k, v) if k != "_typeName" && k != "id" => (k, v) }

    ImportNode(ImportIdentifier(typeName, id), valueMap)
  }

  def convertToImportList(json: JsValue): ImportList = {
    import MyJsonProtocol._
    val map              = json.convertTo[Map[String, Any]]
    val typeName: String = map("_typeName").asInstanceOf[String]
    val id: String       = map("id").asInstanceOf[String]
    val valueMap         = map.collect { case (k, v) if k != "_typeName" && k != "id" => (k, v.asInstanceOf[List[Any]].toVector) }

    ImportList(ImportIdentifier(typeName, id), valueMap)
  }

  def convertToImportRelation(json: JsValue): ImportRelation = {
    import MyJsonProtocol._
    val array    = json.convertTo[JsArray]
    val leftMap  = array.elements.head.convertTo[Map[String, String]]
    val rightMap = array.elements.reverse.head.convertTo[Map[String, String]]
    val left     = ImportRelationSide(ImportIdentifier(leftMap("_typeName"), leftMap("id")), leftMap("fieldName"))
    val right    = ImportRelationSide(ImportIdentifier(rightMap("_typeName"), rightMap("id")), rightMap("fieldName"))

    ImportRelation(left, right)
  }

  def executeImport(project: Project, json: JsValue)(implicit injector: ClientInjector): Future[JsValue] = {
    import MyJsonProtocol._
    import spray.json._

    import scala.concurrent.ExecutionContext.Implicits.global
    val bundle = json.convertTo[ImportBundle]
    val cnt    = bundle.values.elements.length

    val actions = bundle.valueType match {
      case "nodes"     => generateImportNodesDBActions(project, bundle.values.elements.map(convertToImportNode))
      case "relations" => generateImportRelationsDBActions(project, bundle.values.elements.map(convertToImportRelation))
      case "lists"     => generateImportListsDBActions(project, bundle.values.elements.map(convertToImportList))
    }

    val res: Future[Vector[Try[Int]]]                       = runDBActions(project, actions)
    def messageWithOutConnection(tryelem: Try[Any]): String = tryelem.failed.get.getMessage.substring(tryelem.failed.get.getMessage.indexOf(")") + 1)

    res
      .map(vector =>
        vector.zipWithIndex.collect {
          case (elem, idx) if elem.isFailure && idx < cnt  => Map("index" -> idx, "message"         -> messageWithOutConnection(elem)).toJson
          case (elem, idx) if elem.isFailure && idx >= cnt => Map("index" -> (idx - cnt), "message" -> messageWithOutConnection(elem)).toJson
      })
      .map(x => JsArray(x))
  }

  def generateImportNodesDBActions(project: Project, nodes: Vector[ImportNode]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
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

  def generateImportListsDBActions(project: Project, lists: Vector[ImportList]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
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

  //use GCValues for the conversions?

  def isLimitReached(bundle: JsonBundle): Boolean = bundle.size > 1000 // only for testing purposes variable in here

  sealed trait ExportInfo {
    val cursor: Cursor
    val hasNext: Boolean
    def rowPlus(increase: Int): ExportInfo = this match {
      case info: NodeInfo     => info.copy(cursor = info.cursor.copy(row = info.cursor.row + increase))
      case info: ListInfo     => info.copy(cursor = info.cursor.copy(row = info.cursor.row + increase))
      case info: RelationInfo => info.copy(cursor = info.cursor.copy(row = info.cursor.row + increase))
    }

    def cursorAtNextModel: ExportInfo = this match {
      case info: NodeInfo     => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
      case info: ListInfo     => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
      case info: RelationInfo => info.copy(cursor = info.cursor.copy(table = info.cursor.table + 1, row = 0))
    }
  }
  case class NodeInfo(dataResolver: DataResolver, models: List[(Model, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int           = models.length
    val hasNext: Boolean      = cursor.table < length - 1
    lazy val current: Model   = models.find(_._2 == cursor.table).get._1
    lazy val nextModel: Model = models.find(_._2 == cursor.table + 1).get._1
  }

  case class ListInfo(dataResolver: DataResolver, models: List[(Model, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int                                     = models.length
    val listFields: List[(String, TypeIdentifier, Int)] = currentModel.scalarListFields.zipWithIndex.map { case (f, i) => (f.name, f.typeIdentifier, i) }
    val fieldLength: Int                                = listFields.length
    val hasNext: Boolean                                = cursor.table < length - 1
    val hasNextField: Boolean                           = cursor.field < fieldLength - 1
    lazy val currentModel: Model                        = models.find(_._2 == cursor.table).get._1
    lazy val nextModel: Model                           = models.find(_._2 == cursor.table + 1).get._1
    lazy val currentField: String                       = listFields.find(_._3 == cursor.field).get._1
    lazy val nextField: String                          = listFields.find(_._3 == cursor.field + 1).get._1
    lazy val currentTypeIdentifier: TypeIdentifier      = listFields.find(_._3 == cursor.field).get._2
    def arrayPlus(increase: Int): ListInfo              = this.copy(cursor = this.cursor.copy(array = this.cursor.array + increase))
    def cursorAtNextField: ListInfo                     = this.copy(cursor = this.cursor.copy(field = this.cursor.field + 1, array = 0))
  }

  case class RelationInfo(dataResolver: DataResolver, relations: List[(RelationData, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int                     = relations.length
    val hasNext: Boolean                = cursor.table < length - 1
    lazy val current: RelationData      = relations.find(_._2 == cursor.table).get._1
    lazy val nextRelation: RelationData = relations.find(_._2 == cursor.table + 1).get._1
  }

  def executeExport(project: Project, dataResolver: DataResolver, json: JsValue): Future[JsValue] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import spray.json._
    import MyJsonProtocol._

    val start   = JsonBundle(Vector.empty, 0)
    val request = json.convertTo[ExportRequest]
    val response = request.fileType match {
      case "nodes"     => resForCursor(start, NodeInfo(dataResolver, project.models.zipWithIndex, request.cursor))
      case "lists"     => resForCursor(start, ListInfo(dataResolver, project.models.filter(m => m.scalarFields.exists(f => f.isList)).zipWithIndex, request.cursor))
      case "relations" => resForCursor(start, RelationInfo(dataResolver, project.relations.map(r => toRelationData(r, project)).zipWithIndex, request.cursor))
    }
    response.map { x =>
      println(x.toJson)
      x.toJson

    }
  }

  def resForCursor(in: JsonBundle, info: ExportInfo): Future[ResultFormat] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    for {
      result <- resultForTable(in, info)
      x <- result.isFull match {
            case false if info.hasNext  => resForCursor(result.out, info.cursorAtNextModel)
            case false if !info.hasNext => Future.successful(result.copy(cursor = Cursor(-1, -1, -1, -1)))
            case true                   => Future.successful(result)
          }
    } yield x
  }

  def resultForTable(in: JsonBundle, info: ExportInfo): Future[ResultFormat] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    fetchDataItemsPage(info).flatMap { page =>
      val result = serializePage(in, page, info)

      (result.isFull, page.hasMore) match {
        case (false, true)  => resultForTable(in = result.out, info.rowPlus(1000))
        case (false, false) => Future.successful(result)
        case (true, _)      => Future.successful(result)
      }
    }
  }

  case class DataItemsPage(items: Seq[DataItem], hasMore: Boolean) { def itemCount: Int = items.length }
  def fetchDataItemsPage(info: ExportInfo): Future[DataItemsPage] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val queryArguments = QueryArguments(skip = Some(info.cursor.row), after = None, first = Some(1000), None, None, None, None)
    val res: Future[DataItemsPage] = for {
      result <- info match {
                 case x: NodeInfo     => x.dataResolver.loadModelRowsForExport(x.current, Some(queryArguments))
                 case x: ListInfo     => x.dataResolver.loadModelRowsForExport(x.currentModel, Some(queryArguments)) //own select only for list fields?
                 case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
               }
    } yield {
      DataItemsPage(result.items, hasMore = result.hasNextPage)
    }
    res.map { page =>
      info match {
        case info: ListInfo => filterDataItemsPageForLists(page, info)
        case _              => page
      }
    }
  }

  def filterDataItemsPageForLists(in: DataItemsPage, info: ListInfo): DataItemsPage = {
    val items: Seq[DataItem] = in.items

    val itemsWithoutEmptyListsAndNonListFields =
      items.map(item => item.copy(userData = item.userData.collect { case (k, v) if info.listFields.map(_._1).contains(k) && !v.contains("[]") => (k, v) }))

    val res = itemsWithoutEmptyListsAndNonListFields.filter(item => item.userData != Map.empty)
    in.copy(items = res)
  }

  def serializePage(in: JsonBundle, page: DataItemsPage, info: ExportInfo, startOnPage: Int = 0, amount: Int = 1000): ResultFormat = {
    //we are wasting some serialization efforts here when we convert stuff again after backtracking

    val dataItems = page.items.slice(startOnPage, startOnPage + amount)
    val result    = serializeDataItems(in, dataItems, info)
    val noneLeft  = startOnPage + amount >= page.itemCount

    result.isFull match {
      case true if amount == 1 => result
      case false if noneLeft   => result
      case true                => serializePage(in = in, page = page, info, startOnPage, amount / 10)
      case false               => serializePage(in = result.out, page, info.rowPlus(dataItems.length), startOnPage + dataItems.length, amount)
    }
  }

  def serializeDataItems(in: JsonBundle, dataItems: Seq[DataItem], info: ExportInfo): ResultFormat = {

    info match {
      case info: NodeInfo =>
        val bundles          = dataItems.map(item => dataItemToExportNode(item, info))
        val combinedElements = in.jsonElements ++ bundles.flatMap(_.jsonElements).toVector
        val combinedSize = bundles.map(_.size).fold(in.size) { (a, b) =>
          a + b
        }
        val out              = JsonBundle(combinedElements, combinedSize)
        val numberSerialized = dataItems.length

        isLimitReached(out) match {
          case true  => ResultFormat(in, info.cursor, isFull = true)
          case false => ResultFormat(out, info.cursor.copy(row = info.cursor.row + numberSerialized), isFull = false)
        }

      case info: RelationInfo =>
        val bundles          = dataItems.map(item => dataItemToExportRelation(item, info))
        val combinedElements = in.jsonElements ++ bundles.flatMap(_.jsonElements).toVector
        val combinedSize = bundles.map(_.size).fold(in.size) { (a, b) =>
          a + b
        }
        val out              = JsonBundle(combinedElements, combinedSize)
        val numberSerialized = dataItems.length

        isLimitReached(out) match {
          case true  => ResultFormat(in, info.cursor, isFull = true)
          case false => ResultFormat(out, info.cursor.copy(row = info.cursor.row + numberSerialized), isFull = false)
        }

      case info: ListInfo =>
        dataItemsForLists(in, dataItems, info)
    }
  }

  def dataItemsForLists(in: JsonBundle, items: Seq[DataItem], info: ListInfo): ResultFormat = {
    if (items.isEmpty) {
      ResultFormat(in, info.cursor, isFull = false)
    } else {
      val res = dataItemToExportList(in, items.head, info)
      res.isFull match {
        case true  => res
        case false => dataItemsForLists(res.out, items.tail, info)
      }
    }
  }

  def dataItemToExportNode(item: DataItem, info: NodeInfo): JsonBundle = {
    import MyJsonProtocol._
    import spray.json._

    val dataValueMap: UserData                          = item.userData
    val createdAtUpdatedAtMap                           = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
    val withoutImplicitFields: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
    val nonListFieldsWithValues: Map[String, Any]       = withoutImplicitFields.collect { case (k, Some(v)) if !info.current.getFieldByName_!(k).isList => (k, v) }
    val outputMap: Map[String, Any]                     = nonListFieldsWithValues ++ createdAtUpdatedAtMap
    val result: Map[String, Any]                        = Map("_typeName" -> info.current.name, "id" -> item.id) ++ outputMap

    val json = result.toJson
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }

  def dataItemToExportList(in: JsonBundle, item: DataItem, info: ListInfo): ResultFormat = {
    import cool.graph.shared.schema.CustomScalarTypes.parseValueFromString
    val listFieldsWithValues: Map[String, Any] = item.userData.collect { case (k, Some(v)) if info.listFields.map(p => p._1).contains(k) => (k, v) }

    val convertedListFieldsWithValues = listFieldsWithValues.map {
      case (k, v) =>
        val any = parseValueFromString(v.toString, info.listFields.find(_._1 == k).get._2, isList = true)
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

  def serializeFields(in: JsonBundle, identifier: ImportIdentifier, fieldValues: Map[String, Vector[Any]], info: ListInfo): ResultFormat = {
    val result = serializeArray(in, identifier, fieldValues(info.currentField), info)

    result.isFull match {
      case false if info.hasNextField => serializeFields(result.out, identifier, fieldValues, info.cursorAtNextField)
      case false                      => result
      case true                       => result
    }
  }

//  this should have the ability to scale up again, but doing it within one field probably adds too much complexity for now
  def serializeArray(in: JsonBundle, identifier: ImportIdentifier, arrayValues: Vector[Any], info: ListInfo, amount: Int = 1000000): ResultFormat = {
    import MyJsonProtocol._
    import spray.json._

    val values                   = arrayValues.slice(info.cursor.array, info.cursor.array + amount)
    val result: Map[String, Any] = Map("_typeName" -> identifier.typeName, "id" -> identifier.id, info.currentField -> values)
    val json                     = result.toJson
    val combinedElements         = in.jsonElements :+ json
    val combinedSize             = in.size + json.toString.length
    val out                      = JsonBundle(combinedElements, combinedSize)
    val numberSerialized         = values.length
    val noneLeft                 = info.cursor.array + amount >= arrayValues.length

    isLimitReached(out) match {
      case true if amount == 1 => ResultFormat(in, info.cursor, isFull = true)
      case false if noneLeft   => ResultFormat(out, info.cursor.copy(array = 0), isFull = false)
      case false               => serializeArray(out, identifier, arrayValues, info.arrayPlus(numberSerialized), amount)
      case true                => serializeArray(in, identifier, arrayValues, info, amount / 10)
    }
  }

  case class RelationData(relationId: String, leftModel: String, leftField: String, rightModel: String, rightField: String)
  def toRelationData(r: Relation, project: Project): RelationData = {
    RelationData(r.id, r.getModelB_!(project).name, r.getModelBField_!(project).name, r.getModelA_!(project).name, r.getModelAField_!(project).name)
  }

  def dataItemToExportRelation(item: DataItem, info: RelationInfo): JsonBundle = {
    import MyJsonProtocol._
    import spray.json._
    val idA      = item.userData("A").get.toString
    val idB      = item.userData("B").get.toString
    val leftMap  = Map("_typeName" -> info.current.leftModel, "id" -> idB, "fieldName" -> info.current.leftField)
    val rightMap = Map("_typeName" -> info.current.rightModel, "id" -> idA, "fieldName" -> info.current.rightField)

    val json = JsArray(leftMap.toJson, rightMap.toJson)
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }
}

object teststuff {

  def readFile(fileName: String): JsValue = {
    import spray.json._
    val json_string = scala.io.Source
      .fromFile(s"/Users/matthias/repos/github.com/graphcool/closed-source/integration-testing/src/test/scala/cool/graph/bulkimportandexport/$fileName")
      .getLines
      .mkString
    json_string.parseJson
  }
}
