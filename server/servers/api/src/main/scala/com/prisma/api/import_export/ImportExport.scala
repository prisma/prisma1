package com.prisma.api.import_export

import com.prisma.api.connector._
import com.prisma.gc_values.RootGCValue
import com.prisma.shared.models.{Model, Project, Relation}
import com.prisma.util.json.PlaySprayConversions
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}
import play.api.libs.json.{Json, JsValue => PlayJsValue}

package object ImportExport {

  case class ExportRequest(fileType: String, cursor: Cursor)      //{"fileType":"nodes","cursor":{"table":INT,"row":INT,"field":INT,"array":INT}} // TODO make CLI agnostic to this, get rid of field and array columns
  case class Cursor(table: Int, row: Int, field: Int, array: Int) //{"table":INT,"row":INT,"field":INT,"array":INT}
  case class ResultFormat(out: JsonBundle, cursor: Cursor, isFull: Boolean)
  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: String)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: Option[String])
  case class ImportNode(id: String, model: Model, values: RootGCValue)
  case class ImportRelation(left: ImportRelationSide, right: ImportRelationSide)
  case class ImportList(identifier: ImportIdentifier, values: Map[String, Vector[Any]])
  case class JsonBundle(jsonElements: Vector[PlayJsValue], size: Int)
  case class ExportRelationSide(_typeName: String, id: String, fieldName: Option[String])

  case class CreateDataItemImport(project: Project, model: Model, args: ReallyCoolArgs)
  case class CreateRelationRow(project: Project, relation: Relation, a: String, b: String)
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
    lazy val currentFieldModel = currentModelModel.getFieldByName_!(currentField)
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
      r.id,
      r.getModelB_!(project.schema).name,
      r.getModelBField(project.schema).map(_.name),
      r.getModelA_!(project.schema).name,
      r.getModelAField(project.schema).map(_.name)
    )
  }

  case class DataItemsPage(items: Seq[PrismaNode], hasMore: Boolean) { def itemCount: Int = items.length }

  object MyJsonProtocol extends DefaultJsonProtocol {

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

    implicit object PlayJsonFormat extends JsonFormat[PlayJsValue] with PlaySprayConversions {
      override def write(obj: PlayJsValue) = obj.toSpray()

      override def read(json: JsValue) = json.toPlay()
    }

    implicit val jsonBundle: RootJsonFormat[JsonBundle]                 = jsonFormat2(JsonBundle)
    implicit val jsonBundleWithPlay                                     = Json.format[JsonBundle]
    implicit val importBundle: RootJsonFormat[ImportBundle]             = jsonFormat2(ImportBundle)
    implicit val importIdentifier: RootJsonFormat[ImportIdentifier]     = jsonFormat2(ImportIdentifier)
    implicit val importRelationSide: RootJsonFormat[ImportRelationSide] = jsonFormat2(ImportRelationSide)
    implicit val importListValue: RootJsonFormat[ImportList]            = jsonFormat2(ImportList)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat2(ImportRelation)
    implicit val cursor                                                 = Json.format[Cursor]
    implicit val exportRequest                                          = Json.format[ExportRequest]
    implicit val resultFormat                                           = Json.format[ResultFormat]
    implicit val exportRelationSide                                     = Json.format[ExportRelationSide]
  }

}
