package com.prisma.api.connector.sqlite.native

import com.google.protobuf.ByteString
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.rs.{NativeBinding, NodeResult}
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import prisma.getNodeByWhere.GraphqlId.IdValue
import prisma.getNodeByWhere.ValueContainer.PrismaValue
import prisma.getNodeByWhere.{Header, ValueContainer}

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteNativeDataResolver(forwarder: DataResolver)(implicit ec: ExecutionContext) extends DataResolver {
  import com.prisma.shared.models.ProjectJsonFormatter._

  override def project: Project = forwarder.project

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = forwarder.getModelForGlobalId(globalId)

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.getNodeByWhere.GetNodeByWhere(
      Header("GetNodeByWhere"),
      ByteString.copyFromUtf8(projectJson.toString()),
      where.model.name,
      where.fieldName,
      ValueContainer(toPrismaValue(where.fieldGCValue))
    )

    val nodeResult: Option[NodeResult] = NativeBinding.get_node_by_where(input)
    nodeResult.map(prismaNodeFromNodeResult)
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

  def toPrismaValue(value: GCValue): PrismaValue = {
    value match {
      case NullGCValue         => PrismaValue.Null(true)
      case BooleanGCValue(b)   => PrismaValue.Boolean(b)
      case DateTimeGCValue(dt) => PrismaValue.DateTime(isoFormatter.print(dt.withZone(DateTimeZone.UTC)))
      case EnumGCValue(e)      => PrismaValue.Enum(e)
      case FloatGCValue(f)     => PrismaValue.Float(f.floatValue())
      case StringIdGCValue(s)  => PrismaValue.GraphqlId(prisma.getNodeByWhere.GraphqlId(IdValue.String(s)))
      case UuidGCValue(uuid)   => PrismaValue.Uuid(uuid.toString)
      case IntGCValue(i)       => PrismaValue.GraphqlId(prisma.getNodeByWhere.GraphqlId(IdValue.Int(i)))
      case JsonGCValue(j)      => PrismaValue.Json(j.toString())
      case StringGCValue(s)    => PrismaValue.String(s)
      case _                   => sys.error(s"Not supported: $value")
    }
  }
}
