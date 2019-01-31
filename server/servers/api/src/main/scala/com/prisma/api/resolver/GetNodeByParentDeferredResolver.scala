package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, PrismaNode, PrismaNodeWithParent, SelectedFields}
import com.prisma.api.resolver.DeferredTypes.{GetNodeByParentDeferred, GetNodeDeferredResultType, OrderedDeferred, OrderedDeferredFutureResult}
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

class GetNodeByParentDeferredResolver(dataResolver: DataResolver) {
  def resolve(
      orderedDeferreds: Vector[OrderedDeferred[GetNodeByParentDeferred]]
  )(implicit ec: ExecutionContext): Vector[OrderedDeferredFutureResult[GetNodeDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    val headDeferred   = deferreds.head
    val relatedField   = headDeferred.parentField
    val args           = headDeferred.args
    val selectedFields = deferreds.foldLeft(SelectedFields.empty)(_ ++ _.selectedFields)

    // get ids of prismaNodes in related model we need to fetch
    val relatedModelIds = deferreds.map(deferred => deferred.parentNodeId)

    // fetch prismaNodes
    val futurePrismaNodes: Future[Vector[PrismaNodeWithParent]] =
      dataResolver.getRelatedNodes(relatedField, relatedModelIds, args, selectedFields).map(_.flatMap(_.nodes))

    // assign the prismaNode that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[GetNodeDeferredResultType](futurePrismaNodes.map { nodes =>
          prismaNodesToToOneDeferredResultType(dataResolver.project, deferred, nodes)
        }, order)
    }
  }

  private def prismaNodesToToOneDeferredResultType(project: Project,
                                                   deferred: GetNodeByParentDeferred,
                                                   nodes: Vector[PrismaNodeWithParent]): Option[PrismaNode] = {
    def matchesRelation(prismaNodeWithParent: PrismaNodeWithParent, relationSide: String) = prismaNodeWithParent.parentId == deferred.parentNodeId

    nodes.find(node => matchesRelation(node, deferred.parentField.relationSide.toString)).map(_.prismaNode)
  }
}
