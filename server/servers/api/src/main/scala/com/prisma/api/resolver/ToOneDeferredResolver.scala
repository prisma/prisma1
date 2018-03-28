package com.prisma.api.resolver

import com.prisma.api.connector.{DataItem, DataResolver, PrismaNodeWithParent}
import com.prisma.api.resolver.DeferredTypes.{OneDeferredResultType, OrderedDeferred, OrderedDeferredFutureResult, ToOneDeferred}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ToOneDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ToOneDeferred]]): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // get ids of dataitems in related model we need to fetch
    val relatedModelIds = deferreds.map(deferred => IdGCValue(deferred.parentNodeId))

    // fetch dataitems
    val futureprismaNodes = dataResolver.resolveByRelationManyModels(relatedField, relatedModelIds, args).map(_.flatMap(_.nodes))

    // assign the dataitem that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futureprismaNodes.map { items =>
          dataItemsToToOneDeferredResultType(dataResolver.project, deferred, items)
        }, order)
    }

    results
  }

  private def dataItemsToToOneDeferredResultType(project: Project, deferred: ToOneDeferred, nodes: Vector[PrismaNodeWithParent]): Option[DataItem] = {

    def matchesRelation(prismaNodeWithParent: PrismaNodeWithParent, relationSide: String) =
      prismaNodeWithParent.parentId.value == deferred.parentNodeId

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge = deferred.relationField.relation.get.isSameFieldSameModelRelation(project.schema)

    nodes
      .find(node => {
        resolveFromBothSidesAndMerge match {
          case false => matchesRelation(node, deferred.relationField.relationSide.get.toString)
          case true =>
            node.prismaNode.id.value != deferred.parentNodeId && (matchesRelation(node, deferred.relationField.relationSide.get.toString) ||
              matchesRelation(node, deferred.relationField.oppositeRelationSide.get.toString))
        }
      })
      .map(_.prismaNode.toDataItem)
  }
}
