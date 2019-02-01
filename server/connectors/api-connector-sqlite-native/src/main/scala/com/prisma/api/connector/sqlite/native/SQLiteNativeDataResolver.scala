package com.prisma.api.connector.sqlite.native

import com.google.protobuf.ByteString
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.rs.{NativeBinding, NodeResult}
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import prisma.protocol.GraphqlId.IdValue
import prisma.protocol.ValueContainer.PrismaValue
import prisma.protocol.{GraphqlId, Header, Node, ValueContainer}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class SQLiteNativeDataResolver(forwarder: DataResolver)(implicit ec: ExecutionContext) extends DataResolver {
  import com.prisma.shared.models.ProjectJsonFormatter._

  override def project: Project = forwarder.project

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = forwarder.getModelForGlobalId(globalId)

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetNodeByWhereInput(
      Header("GetNodeByWhereInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      where.model.name,
      where.fieldName,
      ValueContainer(toPrismaValue(where.fieldGCValue))
    )

    val nodeResult: Option[(Node, Vector[String])] = NativeBinding.get_node_by_where(input)
    nodeResult.map(x => transformNode(x, where))
  }

  override def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] =
    forwarder.getNodes(model, queryArguments, selectedFields)

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: QueryArguments,
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] =
    forwarder.getRelatedNodes(fromField, fromNodeIds, queryArguments, selectedFields)

  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]] =
    forwarder.getScalarListValues(model, listField, queryArguments)

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] =
    forwarder.getScalarListValuesByNodeIds(model, listField, nodeIds)

  override def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]] =
    forwarder.getRelationNodes(relationTableName, queryArguments)

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = countByTable(table, whereFilter)

  override def countByModel(model: Model, queryArguments: QueryArguments): Future[Int] = countByModel(model, queryArguments)

  def prismaNodeFromNodeResult(nodeResult: NodeResult): PrismaNode = PrismaNode(nodeResult.id, nodeResult.data)

  val isoFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def transformNode(node: (Node, Vector[String]), selector: NodeSelector): PrismaNode = {
    val idField = selector.model.idField_!
    val idIndex = node._2.indexWhere(idField.name == _)
    val idValue = toGcValue(node._1.values.toVector(idIndex).prismaValue)

    val rootMap = node._1.values.zipWithIndex.foldLeft(mutable.Map.empty[String, GCValue]) {
      case (acc, (value: ValueContainer, index: Int)) =>
        acc += (node._2(index) -> toGcValue(value.prismaValue))
    }

    PrismaNode(idValue.asInstanceOf[IdGCValue], RootGCValue(rootMap.toMap))
  }

  def toGcValue(value: ValueContainer.PrismaValue): GCValue = {
    value match {
      case PrismaValue.Empty                => NullGCValue
      case PrismaValue.Boolean(b: Boolean)  => BooleanGCValue(b)
      case PrismaValue.DateTime(dt: String) => DateTimeGCValue(DateTime.parse(dt))
      case PrismaValue.Enum(e: String)      => EnumGCValue(e)
      case PrismaValue.Float(f: Float)      => FloatGCValue(f)
      case PrismaValue.GraphqlId(id: GraphqlId) =>
        id.idValue match {
          case IdValue.String(s) => StringIdGCValue(s)
          case IdValue.Int(i)    => IntGCValue(i.toInt)
          case _                 => sys.error("empty protobuf")
        }
      case PrismaValue.Int(i: Int)        => IntGCValue(i)
      case PrismaValue.Json(j: String)    => JsonGCValue(Json.parse(j))
      case PrismaValue.Null(_)            => NullGCValue
      case PrismaValue.Relation(r: Long)  => ??? // What are we supposed to do here?
      case PrismaValue.String(s: String)  => StringGCValue(s)
      case PrismaValue.Uuid(uuid: String) => UuidGCValue.parse(uuid).get
    }
  }

  def toPrismaValue(value: GCValue): PrismaValue = {
    value match {
      case NullGCValue         => PrismaValue.Null(true)
      case BooleanGCValue(b)   => PrismaValue.Boolean(b)
      case DateTimeGCValue(dt) => PrismaValue.DateTime(isoFormatter.print(dt.withZone(DateTimeZone.UTC)))
      case EnumGCValue(e)      => PrismaValue.Enum(e)
      case FloatGCValue(f)     => PrismaValue.Float(f.floatValue())
      case StringIdGCValue(s)  => PrismaValue.GraphqlId(prisma.protocol.GraphqlId(IdValue.String(s)))
      case UuidGCValue(uuid)   => PrismaValue.Uuid(uuid.toString)
      case IntGCValue(i)       => PrismaValue.GraphqlId(prisma.protocol.GraphqlId(IdValue.Int(i)))
      case JsonGCValue(j)      => PrismaValue.Json(j.toString())
      case StringGCValue(s)    => PrismaValue.String(s)
      case _                   => sys.error(s"Not supported: $value")
    }
  }
}
