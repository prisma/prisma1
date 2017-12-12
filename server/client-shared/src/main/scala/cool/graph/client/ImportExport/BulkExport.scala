package cool.graph.client.ImportExport

import cool.graph.DataItem
import cool.graph.Types.UserData
import cool.graph.client.ClientInjector
import cool.graph.client.database.{DataResolver, QueryArguments}
import cool.graph.shared.models.Project
import spray.json.JsValue
import spray.json._
import scala.concurrent.ExecutionContext.Implicits.global
import MyJsonProtocol._
import scala.concurrent.Future

class BulkExport(implicit clientInjector: ClientInjector) {

  def executeExport(project: Project, dataResolver: DataResolver, json: JsValue): Future[JsValue] = {
    val start   = JsonBundle(Vector.empty, 0)
    val request = json.convertTo[ExportRequest]
    val response = request.fileType match {
      case "nodes"     => resForCursor(start, NodeInfo(dataResolver, project.models.zipWithIndex, request.cursor))
      case "lists"     => resForCursor(start, ListInfo(dataResolver, project.models.filter(m => m.scalarFields.exists(f => f.isList)).zipWithIndex, request.cursor))
      case "relations" => resForCursor(start, RelationInfo(dataResolver, project.relations.map(r => toRelationData(r, project)).zipWithIndex, request.cursor))
    }
    response.map(_.toJson)
  }

  private def isLimitReached(bundle: JsonBundle)(implicit clientInjector: ClientInjector): Boolean = bundle.size > clientInjector.maxImportExportSize

  private def resForCursor(in: JsonBundle, info: ExportInfo): Future[ResultFormat] = {
    for {
      result <- resultForTable(in, info)
      x <- result.isFull match {
            case false if info.hasNext  => resForCursor(result.out, info.cursorAtNextModel)
            case false if !info.hasNext => Future.successful(result.copy(cursor = Cursor(-1, -1, -1, -1)))
            case true                   => Future.successful(result)
          }
    } yield x
  }

  private def resultForTable(in: JsonBundle, info: ExportInfo): Future[ResultFormat] = {
    fetchDataItemsPage(info).flatMap { page =>
      val result = serializePage(in, page, info)

      (result.isFull, page.hasMore) match {
        case (false, true)  => resultForTable(in = result.out, info.rowPlus(1000))
        case (false, false) => Future.successful(result)
        case (true, _)      => Future.successful(result)
      }
    }
  }

  private def fetchDataItemsPage(info: ExportInfo): Future[DataItemsPage] = {
    val queryArguments = QueryArguments(skip = Some(info.cursor.row), after = None, first = Some(1000), None, None, None, None)
    val dataItemsPage: Future[DataItemsPage] = for {
      result <- info match {
                 case x: NodeInfo     => x.dataResolver.loadModelRowsForExport(x.current, Some(queryArguments))
                 case x: ListInfo     => x.dataResolver.loadModelRowsForExport(x.currentModel, Some(queryArguments))
                 case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
               }
    } yield {
      DataItemsPage(result.items, hasMore = result.hasNextPage)
    }
    dataItemsPage.map { page =>
      info match {
        case info: ListInfo => filterDataItemsPageForLists(page, info)
        case _              => page
      }
    }
  }

  private def filterDataItemsPageForLists(in: DataItemsPage, info: ListInfo): DataItemsPage = {
    val itemsWithoutEmptyListsAndNonListFieldsInUserData =
      in.items.map(item => item.copy(userData = item.userData.collect { case (k, v) if info.listFields.map(_._1).contains(k) && !v.contains("[]") => (k, v) }))

    val itemsWithSomethingLeftToInsert = itemsWithoutEmptyListsAndNonListFieldsInUserData.filter(item => item.userData != Map.empty)
    in.copy(items = itemsWithSomethingLeftToInsert)
  }

  private def serializePage(in: JsonBundle, page: DataItemsPage, info: ExportInfo, startOnPage: Int = 0, amount: Int = 1000): ResultFormat = {
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

  private def serializeDataItems(in: JsonBundle, dataItems: Seq[DataItem], info: ExportInfo): ResultFormat = {
    def serializeNonListItems(info: ExportInfo): ResultFormat = {
      val bundles = info match {
        case info: NodeInfo     => dataItems.map(item => dataItemToExportNode(item, info))
        case info: RelationInfo => dataItems.map(item => dataItemToExportRelation(item, info))
        case _: ListInfo        => sys.error("shouldnt happen")
      }
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
    }

    info match {
      case info: NodeInfo     => serializeNonListItems(info)
      case info: RelationInfo => serializeNonListItems(info)
      case info: ListInfo     => dataItemsForLists(in, dataItems, info)
    }
  }

  private def dataItemsForLists(in: JsonBundle, items: Seq[DataItem], info: ListInfo): ResultFormat = {
    if (items.isEmpty) {
      ResultFormat(in, info.cursor, isFull = false)
    } else {
      val result = dataItemToExportList(in, items.head, info)
      result.isFull match {
        case true  => result
        case false => dataItemsForLists(result.out, items.tail, info)
      }
    }
  }

  private def dataItemToExportNode(item: DataItem, info: NodeInfo): JsonBundle = {
    val dataValueMap: UserData                        = item.userData
    val createdAtUpdatedAtMap                         = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
    val withoutHiddenFields: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
    val nonListFieldsWithValues: Map[String, Any]     = withoutHiddenFields.collect { case (k, Some(v)) if !info.current.getFieldByName_!(k).isList => (k, v) }
    val outputMap: Map[String, Any]                   = nonListFieldsWithValues ++ createdAtUpdatedAtMap
    val result: Map[String, Any]                      = Map("_typeName" -> info.current.name, "id" -> item.id) ++ outputMap

    val json = result.toJson
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }

  private def dataItemToExportList(in: JsonBundle, item: DataItem, info: ListInfo)(implicit clientInjector: ClientInjector): ResultFormat = {
    import cool.graph.shared.schema.CustomScalarTypes.parseValueFromString
    val listFieldsWithValues: Map[String, Any] = item.userData.collect { case (k, Some(v)) if info.listFields.map(p => p._1).contains(k) => (k, v) }

    val convertedListFieldsWithValues = listFieldsWithValues.map {
      case (k, v) =>
        val any = parseValueFromString(v.toString, info.listFields.find(_._1 == k).get._2, isList = true)
        val vector = any match {
          case Some(Some(x)) => x.asInstanceOf[Vector[Any]]
          case x             => sys.error("Failure reading a Listvalue from DB: " + x)
        }
        (k, vector)
    }

    val importIdentifier: ImportIdentifier = ImportIdentifier(info.currentModel.name, item.id)
    serializeFields(in, importIdentifier, convertedListFieldsWithValues, info)
  }

  private def serializeFields(in: JsonBundle, identifier: ImportIdentifier, fieldValues: Map[String, Vector[Any]], info: ListInfo): ResultFormat = {
    val result = serializeArray(in, identifier, fieldValues(info.currentField), info)

    result.isFull match {
      case false if info.hasNextField => serializeFields(result.out, identifier, fieldValues, info.cursorAtNextField)
      case false                      => result
      case true                       => result
    }
  }

  private def serializeArray(in: JsonBundle, identifier: ImportIdentifier, arrayValues: Vector[Any], info: ListInfo, amount: Int = 1000000): ResultFormat = {
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

  private def dataItemToExportRelation(item: DataItem, info: RelationInfo): JsonBundle = {
    val idA      = item.userData("A").get.toString
    val idB      = item.userData("B").get.toString
    val leftMap  = Map("_typeName" -> info.current.leftModel, "id" -> idB, "fieldName" -> info.current.leftField)
    val rightMap = Map("_typeName" -> info.current.rightModel, "id" -> idA, "fieldName" -> info.current.rightField)

    val json = JsArray(leftMap.toJson, rightMap.toJson)
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }
}
