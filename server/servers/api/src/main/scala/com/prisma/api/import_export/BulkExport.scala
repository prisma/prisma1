package com.prisma.api.import_export

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{DataResolver, PrismaNode, QueryArguments}
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.gc_values.{GraphQLIdGCValue, JsonGCValue, ListGCValue, StringGCValue}
import com.prisma.shared.models.Project
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkExport(project: Project)(implicit apiDependencies: ApiDependencies) {

  val maxImportExportSize: Int = apiDependencies.maxImportExportSize

  def executeExport(dataResolver: DataResolver, json: JsValue): Future[JsValue] = executeExport(dataResolver, json.as[ExportRequest])

  def executeExport(dataResolver: DataResolver, request: ExportRequest): Future[JsValue] = {
    val start           = JsonBundle(Vector.empty, 0)
    val hasListFields   = project.models.flatMap(_.scalarListFields).nonEmpty
    val zippedRelations = RelationInfo(dataResolver, project.relations.map(r => toRelationData(r, project)).zipWithIndex, request.cursor)

    val listFieldTableNames: List[(String, String, Int)] =
      project.models.flatMap(m => m.scalarListFields.map(f => (m.name, f.name))).zipWithIndex.map { case ((a, b), c) => (a, b, c) }

    val response = request.fileType match {
      case "nodes" if project.models.nonEmpty        => resForCursor(start, NodeInfo(dataResolver, project.models.zipWithIndex, request.cursor))
      case "lists" if hasListFields                  => resForCursor(start, ListInfo(dataResolver, listFieldTableNames, request.cursor))
      case "relations" if project.relations.nonEmpty => resForCursor(start, zippedRelations)
      case _                                         => Future.successful(ResultFormat(start, Cursor(-1, -1, -1, -1), isFull = false))
    }

    response.map(Json.toJson(_))
  }

  private def isLimitReached(bundle: JsonBundle): Boolean = bundle.size > maxImportExportSize

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
    for {
      result <- info match {
                 case x: NodeInfo     => x.dataResolver.loadModelRowsForExport(x.current, Some(queryArguments))
                 case x: ListInfo     => x.dataResolver.loadListRowsForExport(x.currentModelModel, x.currentFieldModel, Some(queryArguments))
                 case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
               }
    } yield {
      DataItemsPage(result.nodes, hasMore = result.hasNextPage)
    }
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

  private def serializeDataItems(in: JsonBundle, dataItems: Seq[PrismaNode], info: ExportInfo): ResultFormat = {
    val bundles: Seq[JsonBundle] = info match {
      case info: NodeInfo     => dataItems.map(item => dataItemToExportNode(item, info))
      case info: RelationInfo => dataItems.map(item => dataItemToExportRelation(item, info))
      case info: ListInfo     => dataItemToExportList(dataItems, info)
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

  def dataItemToExportList(dataItems: Seq[PrismaNode], info: ListInfo): Vector[JsonBundle] = {
    import GCValueJsonFormatter.GcValueWrites
    val distinctIds = dataItems.map(_.id).distinct.toVector
    distinctIds.map { id =>
      val itemsForId    = dataItems.filter(_.id == id).toVector
      val asListGcValue = ListGCValue(itemsForId.map(_.data.map("value")))

      // the old implementation directly passed the JSON as String instead directly embedding it as JSON. Reproducing this behaviour here.
      val blackMagic = ListGCValue(asListGcValue.values.map {
        case x: JsonGCValue => StringGCValue(x.value.toString)
        case x              => x
      })

      val json = Json.obj("_typeName" -> info.currentModel, "id" -> id, info.currentField -> blackMagic)
      JsonBundle(Vector(json), json.toString.length)
    }
  }

  private def dataItemToExportNode(item: PrismaNode, info: NodeInfo): JsonBundle = {
    import GCValueJsonFormatter.RootGcValueWritesWithoutNulls
    val jsonForNode = Json.toJsObject(item.data)
    val jsonObj     = Json.obj("_typeName" -> info.current.name, "id" -> item.id) ++ jsonForNode
    JsonBundle(jsonElements = Vector(jsonObj), size = jsonObj.toString.length)
  }

  private def dataItemToExportRelation(item: PrismaNode, info: RelationInfo): JsonBundle = {
    val idA       = item.data.map("A").asInstanceOf[GraphQLIdGCValue].value
    val idB       = item.data.map("B").asInstanceOf[GraphQLIdGCValue].value
    val leftSide  = ExportRelationSide(info.current.modelBName, idB, info.current.fieldBName)
    val rightSide = ExportRelationSide(info.current.modelAName, idA, info.current.fieldAName)

    val json = JsArray(Seq(Json.toJson(leftSide), Json.toJson(rightSide)))
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }
}
