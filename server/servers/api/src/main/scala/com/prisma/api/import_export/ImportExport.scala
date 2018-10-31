package com.prisma.api.import_export

import com.prisma.api.connector.{DataResolver, PrismaArgs}
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, Project, Relation, ScalarField}
import play.api.libs.json._

import scala.util.Success

package object ImportExport {

  // EXPORT
  case class ExportRequest(fileType: String, cursor: Cursor) //{"fileType":"nodes","cursor":{"table":INT,"row":INT,"field":INT,"array":INT}} // TODO make CLI agnostic to this, get rid of field and array columns
  case class Cursor(table: Int, row: Int)                    //{"table":INT,"row":INT}
  case class ResultFormat(out: JsonBundle, cursor: Cursor, isFull: Boolean)
  case class JsonBundle(jsonElements: Vector[JsValue], size: Int)
  case class ExportRelationSide(_typeName: String, id: IdGCValue, fieldName: Option[String])

  // IMPORT
  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: IdGCValue)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: Option[String])
  case class ImportNode(id: IdGCValue, model: Model, values: RootGCValue)
  case class ImportRelation(left: ImportRelationSide, right: ImportRelationSide)
  case class ImportList(identifier: ImportIdentifier, field: ScalarField, values: ListGCValue)

  // TEMP STRUCTURES
  case class CreateDataItemImport(project: Project, model: Model, args: PrismaArgs)
  case class CreateRelationRow(project: Project, relation: Relation, a: IdGCValue, b: IdGCValue)
  case class PushScalarListImport(project: Project, tableName: String, id: String, values: Vector[Any])

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
    val length: Int         = models.length
    val hasNext: Boolean    = cursor.table < length - 1
    lazy val current: Model = models.find(_._2 == cursor.table).get._1
  }

  case class ListInfo(dataResolver: DataResolver, listFieldTables: List[(String, String, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int               = listFieldTables.length
    val hasNext: Boolean          = cursor.table < length - 1
    lazy val currentModel: String = listFieldTables.find(_._3 == cursor.table).get._1
    lazy val currentField: String = listFieldTables.find(_._3 == cursor.table).get._2
    lazy val currentTable: String = s"${currentModel}_$currentField"

    lazy val currentModelModel = dataResolver.project.schema.getModelByName_!(currentModel)
    lazy val currentFieldModel = currentModelModel.getScalarFieldByName_!(currentField)
  }

  case class RelationInfo(dataResolver: DataResolver, relations: List[(RelationData, Int)], cursor: Cursor) extends ExportInfo {
    val length: Int                = relations.length
    val hasNext: Boolean           = cursor.table < length - 1
    lazy val current: RelationData = relations.find(_._2 == cursor.table).get._1
  }

  case class RelationData(relationId: String, modelBName: String, fieldBName: Option[String], modelAName: String, fieldAName: Option[String]) {
    require(fieldBName.isDefined || fieldAName.isDefined)
  }

  def toRelationData(r: Relation, project: Project): RelationData = {
    RelationData(
      r.relationTableName,
      r.modelB.name,
      if (r.modelBField.isHidden) None else Some(r.modelBField.name),
      r.modelA.name,
      if (r.modelAField.isHidden) None else Some(r.modelAField.name)
    )
  }

  case class PrismaNodesPage(items: Seq[JsValue], hasMore: Boolean) { def itemCount: Int = items.length }

  object MyJsonProtocol {
    val cursorReads = Json.reads[Cursor]
    val cursorWrites = new Writes[Cursor] {
      override def writes(o: Cursor): JsValue = {
        val dummyValue = if (o.table == -1 && o.row == -1) -1 else 0
        Json.obj("table" -> o.table, "row" -> o.row, "field" -> dummyValue, "array" -> dummyValue)
      }
    }
    val idWrites = new Writes[IdGCValue] {
      override def writes(o: IdGCValue): JsValue = o match {
        case id: UuidGCValue     => JsString(id.value.toString)
        case id: StringIdGCValue => JsString(id.value)
        case id: IntGCValue      => JsNumber(id.value)
      }
    }
    val idReads = new Reads[IdGCValue] {
      override def reads(json: JsValue): JsResult[IdGCValue] = {
        val result = json match {
          case id: JsNumber => IntGCValue(id.value.toInt)
          case id: JsString => stringToIdGcValue(id.value)
          case x            => sys.error("An id should always be of type JsNumber or JsString. " + x)
        }
        JsSuccess(result)
      }

      private def stringToIdGcValue(str: String): IdGCValue = {
        UuidGCValue.parse(str) match {
          case Success(id) => id
          case _           => StringIdGCValue(str)
        }
      }
    }

    implicit val idFormat           = Format(idReads, idWrites)
    implicit val cursorFormat       = Format(cursorReads, cursorWrites)
    implicit val jsonBundle         = Json.format[JsonBundle]
    implicit val importBundle       = Json.format[ImportBundle]
    implicit val importIdentifier   = Json.format[ImportIdentifier]
    implicit val importRelationSide = Json.format[ImportRelationSide]
    implicit val importRelation     = Json.format[ImportRelation]
    implicit val exportRequest      = Json.format[ExportRequest]
    implicit val resultFormat       = Json.format[ResultFormat]
    implicit val exportRelationSide = Json.format[ExportRelationSide]

  }

}
