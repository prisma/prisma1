package com.prisma.api.connector.sqlite.native

import com.google.protobuf.ByteString
import com.prisma.api.connector._
import com.prisma.gc_values.{IdGCValue, StringIdGCValue}
import com.prisma.rs.NativeBinding
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}
import play.api.libs.json.Json
import prisma.getNodeByWhere.Header

import scala.concurrent.Future

case class SQLiteNativeDataResolver(forwarder: DataResolver) extends DataResolver {
  import com.prisma.shared.models.ProjectJsonFormatter._

  override def project: Project = forwarder.project

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = ???

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val projectJson = Json.toJson(project)
    val gcValueJson = Json.toJson(where.fieldGCValue)

    val input = prisma.getNodeByWhere.GetNodeByWhere(
      Header("GetNodeByWhere"),
      ByteString.copyFromUtf8(projectJson.toString()),
      where.model.name,
      where.fieldName,
      ByteString.copyFromUtf8(gcValueJson.toString())
    )

    NativeBinding.get_node_by_where(input)
    Future.successful(None)
  }

  override def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = ???

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: QueryArguments,
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]] = ???

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = ???

  override def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]] = ???

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = ???

  override def countByModel(model: Model, queryArguments: QueryArguments): Future[Int] = ???
}
