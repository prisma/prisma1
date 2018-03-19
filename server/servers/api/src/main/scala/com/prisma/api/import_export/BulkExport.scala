package com.prisma.api.import_export

import java.sql.Timestamp

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{DataItem, DataResolver, QueryArguments}
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Project, TypeIdentifier}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import spray.json.{JsValue, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkExport(project: Project)(implicit apiDependencies: ApiDependencies) {

  val maxImportExportSize: Int = apiDependencies.maxImportExportSize

  def executeExport(dataResolver: DataResolver, json: JsValue): Future[JsValue] = {

    val start           = JsonBundle(Vector.empty, 0)
    val request         = json.convertTo[ExportRequest]
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

    response.map(_.toJson)
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
                 case x: ListInfo     => x.dataResolver.loadListRowsForExport(x.currentTable, Some(queryArguments))
                 case x: RelationInfo => x.dataResolver.loadRelationRowsForExport(x.current.relationId, Some(queryArguments))
               }
    } yield {
      DataItemsPage(result.items, hasMore = result.hasNextPage)
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

  private def serializeDataItems(in: JsonBundle, dataItems: Seq[DataItem], info: ExportInfo): ResultFormat = {
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

  def dataItemToExportList(dataItems: Seq[DataItem], info: ListInfo): Vector[JsonBundle] = {
    val outputs: Seq[(Id, Any)] = project.schema.getModelByName_!(info.currentModel).getFieldByName_!(info.currentField).typeIdentifier match {
      case TypeIdentifier.DateTime => dataItems.map(item => item.id -> dateTimeToISO8601(item.userData("value").get))
      case TypeIdentifier.Float    => dataItems.map(item => item.id -> item.userData("value").get.toString.toDouble)
      case _                       => dataItems.map(item => item.id -> item.userData("value").get)
    }

    val distinctIds = outputs.map(_._1).distinct

    val x = distinctIds.map { id =>
      val values: Seq[Any]         = outputs.filter(_._1 == id).map(_._2)
      val result: Map[String, Any] = Map("_typeName" -> info.currentModel, "id" -> id, info.currentField -> values)
      val json                     = result.toJson
      val combinedSize             = json.toString.length

      JsonBundle(Vector(json), combinedSize)
    }
    Vector.empty ++ x
  }

  private def dataItemToExportNode(item: DataItem, info: NodeInfo): JsonBundle = {
    val dataValueMap                                  = item.userData
    val createdAtUpdatedAtMap                         = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
    val withoutHiddenFields: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
    val nonListFieldsWithValues: Map[String, Any]     = withoutHiddenFields.collect { case (k, Some(v)) if !info.current.getFieldByName_!(k).isList => (k, v) }
    val outputMap: Map[String, Any]                   = nonListFieldsWithValues ++ createdAtUpdatedAtMap

    val mapWithCorrectDateTimeFormat = outputMap.map {
      case (k, v) if k == "createdAt" || k == "updatedAt"                                       => (k, dateTimeToISO8601(v))
      case (k, v) if info.current.getFieldByName_!(k).typeIdentifier == TypeIdentifier.DateTime => (k, dateTimeToISO8601(v))
      case (k, v)                                                                               => (k, v)
    }

    val result: Map[String, Any] = Map("_typeName" -> info.current.name, "id" -> item.id) ++ mapWithCorrectDateTimeFormat
    val json                     = result.toJson

    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }

  private def dateTimeToISO8601(v: Any) = v.isInstanceOf[Timestamp] match {
    case true  => DateTime.parse(v.asInstanceOf[Timestamp].toString, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC())
    case false => new DateTime(v.asInstanceOf[String], DateTimeZone.UTC)
  }

  private def dataItemToExportRelation(item: DataItem, info: RelationInfo): JsonBundle = {
    val idA      = item.userData("A").get.toString.trim
    val idB      = item.userData("B").get.toString.trim
    val leftMap  = ExportRelationSide(info.current.modelBName, idB, info.current.fieldBName)
    val rightMap = ExportRelationSide(info.current.modelAName, idA, info.current.fieldAName)

    val json = JsArray(leftMap.toJson, rightMap.toJson)
    JsonBundle(jsonElements = Vector(json), size = json.toString.length)
  }
}
