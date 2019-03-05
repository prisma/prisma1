package com.prisma.api.connector.sqlite.native

import com.google.protobuf.ByteString
import com.prisma.api.connector.{EveryRelatedNode, _}
import com.prisma.gc_values._
import com.prisma.rs.{NativeBinding, NodeResult}
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import prisma.protocol
import prisma.protocol.GraphqlId.IdValue
import prisma.protocol.{GraphqlId, Node}
import prisma.protocol.ValueContainer.PrismaValue

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class SQLiteNativeDataResolver(forwarder: DataResolver)(implicit ec: ExecutionContext) extends DataResolver {
  import com.prisma.shared.models.ProjectJsonFormatter._
  import NativeUtils._

  override def project: Project = forwarder.project

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = forwarder.getModelForGlobalId(globalId)

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetNodeByWhereInput(
      protocol.Header("GetNodeByWhereInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      where.model.dbName,
      where.fieldName,
      protocol.ValueContainer(toPrismaValue(where.fieldGCValue)),
      toPrismaSelectedFields(selectedFields)
    )

    val nodeResult: Option[(protocol.Node, Vector[String])] = NativeBinding.get_node_by_where(input)
    nodeResult.map(x => transformNode(x, where.model))
  }

  override def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetNodesInput(
      protocol.Header("GetNodesInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      model.dbName,
      toPrismaArguments(queryArguments),
      toPrismaSelectedFields(selectedFields)
    )

    val nodeResult: (Vector[Node], Vector[String]) = NativeBinding.get_nodes(input)
    ResolverResult(queryArguments, nodeResult._1.map(x => transformNode((x, nodeResult._2), model)))
  }

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: QueryArguments,
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = {
//    val projectJson = Json.toJson(project)
//    val input = prisma.protocol.GetRelatedNodesInput(
//      protocol.Header("GetRelatedNodesInput"),
//      ByteString.copyFromUtf8(projectJson.toString()),
//      fromField.model.dbName,
//      toRelationalField(fromField),
//      fromNodeIds.map(f => toPrismaValue(f).asInstanceOf[GraphqlId]),
//      toPrismaArguments(queryArguments),
//      toPrismaSelectedFields(selectedFields)
//    )
//
//    val nodeResult: (Vector[Node], Vector[String]) = NativeBinding.get_related_nodes(input)
//    ResolverResult(queryArguments, nodeResult._1.map(x => transformRelatedNode((x, nodeResult._2), model)))
    forwarder.getRelatedNodes(fromField, fromNodeIds, queryArguments, selectedFields)
  }

  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]] =
    forwarder.getScalarListValues(model, listField, queryArguments)

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] =
    forwarder.getScalarListValuesByNodeIds(model, listField, nodeIds)

  override def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]] =
    forwarder.getRelationNodes(relationTableName, queryArguments)

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = forwarder.countByTable(table, whereFilter)

  override def countByModel(model: Model, queryArguments: QueryArguments): Future[Int] = forwarder.countByModel(model, queryArguments)

}
