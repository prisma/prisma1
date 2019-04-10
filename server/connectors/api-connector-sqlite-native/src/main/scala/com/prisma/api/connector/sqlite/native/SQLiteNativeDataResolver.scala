package com.prisma.api.connector.sqlite.native

import com.google.protobuf.ByteString
import com.prisma.api.connector._
import com.prisma.api.helpers.SkipAndLimit
import com.prisma.gc_values._
import com.prisma.rs.NativeBinding
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}
import play.api.libs.json.Json
import prisma.protocol
import prisma.protocol.Node
import prisma.protocol.ValueContainer.PrismaValue.GraphqlId

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteNativeDataResolver(delegate: DataResolver)(implicit ec: ExecutionContext) extends DataResolver {
  import NativeUtils._
  import com.prisma.shared.models.ProjectJsonFormatter._
  import com.prisma.api.helpers.LimitClauseHelper._

  override def project: Project = delegate.project

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = delegate.getModelForGlobalId(globalId)

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetNodeByWhereInput(
      protocol.Header("GetNodeByWhereInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      where.model.name,
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
      model.name,
      toPrismaArguments(queryArguments),
      toPrismaSelectedFields(selectedFields)
    )

    val nodeResult: (Vector[Node], Vector[String]) = NativeBinding.get_nodes(input)
    ResolverResult(queryArguments, nodeResult._1.map(x => transformNode((x, nodeResult._2), model)))
  }

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: QueryArguments,
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetRelatedNodesInput(
      header = protocol.Header("GetRelatedNodesInput"),
      projectJson = ByteString.copyFromUtf8(projectJson.toString()),
      modelName = fromField.model.name,
      fromField = fromField.name,
      fromNodeIds = fromNodeIds.map(f => toPrismaValue(f).asInstanceOf[GraphqlId].value),
      queryArguments = toPrismaArguments(queryArguments),
      selectedFields = toPrismaSelectedFields(selectedFields)
    )

    val nodeResult: (Vector[Node], Vector[String]) = NativeBinding.get_related_nodes(input)
    val nodes                                      = nodeResult._1
    val columnNames                                = nodeResult._2
    val mappedNodes: Vector[PrismaNodeWithParent] = nodes.map { pbNode =>
      PrismaNodeWithParent(
        toIdGcValue(pbNode.parentId.getOrElse(sys.error("Expected get_related_nodes to return parent IDs alongside nodes."))),
        transformNode((pbNode, columnNames), fromField.relatedModel_!)
      )
    }

    val itemGroupsByModelId = mappedNodes.groupBy(_.parentId)
    fromNodeIds.map { id =>
      itemGroupsByModelId.find(_._1 == id) match {
        case Some((_, itemsForId)) => ResolverResult(queryArguments, itemsForId, parentModelId = Some(id))
        case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
      }
    }

//    forwarder.getRelatedNodes(fromField, fromNodeIds, queryArguments, selectedFields)
  }

  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]] = {
    Future.successful(ResolverResult(Vector.empty))
  }

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = Future {
    val projectJson = Json.toJson(project)
    val input = prisma.protocol.GetScalarListValuesByNodeIds(
      header = protocol.Header("GetRelatedNodesInput"),
      projectJson = ByteString.copyFromUtf8(projectJson.toString()),
      modelName = model.name,
      listField = listField.name,
      nodeIds = nodeIds.map(f => toPrismaValue(f).asInstanceOf[GraphqlId].value)
    )

    val result = NativeBinding.get_scalar_list_values_by_node_ids(input)
    result.map { protoValue =>
      ScalarListValues(
        nodeId = toIdGcValue(protoValue.nodeId),
        value = ListGCValue(protoValue.values.map(v => toGcValue(v.prismaValue)).toVector)
      )
    }.toVector
  }

  override def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]] = {
    Future.successful(ResolverResult(Vector.empty))
  }

  override def countByTable(table: String): Future[Int] = Future {
    val projectJson = Json.toJson(project)

    val input = prisma.protocol.CountByTableInput(
      protocol.Header("CountByTableInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      table
    )

    NativeBinding.count_by_table(input)
  }

  override def countByModel(model: Model, queryArguments: QueryArguments): Future[Int] = Future {
    val projectJson = Json.toJson(project)

    val input = prisma.protocol.CountByModelInput(
      protocol.Header("CountByModelInput"),
      ByteString.copyFromUtf8(projectJson.toString()),
      model.name,
      toPrismaArguments(queryArguments)
    )

    val count = NativeBinding.count_by_model(input)
    val SkipAndLimit(_, limit) = skipAndLimitValues(queryArguments)

    val result = limit match {
      case Some(lim) => if (count > (lim - 1)) count - 1 else count
      case None => count
    }

    Math.max(result, 0)
  }

}
