package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, PrismaNode, PrismaNodeWithParent}
import com.prisma.api.resolver.DeferredTypes.{OneDeferredResultType, OrderedDeferred, OrderedDeferredFutureResult, ToOneDeferred}
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

class ToOneDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ToOneDeferred]],
              executionContext: ExecutionContext): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    implicit val ec: ExecutionContext = executionContext
    val deferreds                     = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // get ids of prismaNodes in related model we need to fetch
    val relatedModelIds = deferreds.map(deferred => deferred.parentNodeId)

    // fetch prismaNodes
    val futurePrismaNodes: Future[Vector[PrismaNodeWithParent]] =
      dataResolver.resolveByRelationManyModels(relatedField, relatedModelIds, args).map(_.flatMap(_.nodes))

    // assign the prismaNode that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futurePrismaNodes.map { nodes =>
          prismaNodesToToOneDeferredResultType(dataResolver.project, deferred, nodes)
        }, order)
    }
  }

  private def prismaNodesToToOneDeferredResultType(project: Project, deferred: ToOneDeferred, nodes: Vector[PrismaNodeWithParent]): Option[PrismaNode] = {

    def matchesRelation(prismaNodeWithParent: PrismaNodeWithParent, relationSide: String) =
      prismaNodeWithParent.parentId == deferred.parentNodeId

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge = deferred.relationField.relation.get.isSameFieldSameModelRelation

    nodes
      .find(node => {
        resolveFromBothSidesAndMerge match {
          case false => matchesRelation(node, deferred.relationField.relationSide.get.toString)
          case true =>
            node.prismaNode.id != deferred.parentNodeId && (matchesRelation(node, deferred.relationField.relationSide.get.toString) ||
              matchesRelation(node, deferred.relationField.oppositeRelationSide.get.toString))
        }
      })
      .map(_.prismaNode)
  }
}
