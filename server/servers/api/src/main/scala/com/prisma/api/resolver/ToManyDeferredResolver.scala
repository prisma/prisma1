package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

class ToManyDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ToManyDeferred]],
              executionContext: ExecutionContext): Vector[OrderedDeferredFutureResult[RelayConnectionOutputType]] = {
    implicit val ec: ExecutionContext = executionContext
    val deferreds                     = orderedDeferreds.map(_.deferred)

    // Check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // Get ids of nodes in related model we need to fetch (actual rows of data)
    val relatedModelInstanceIds: Vector[IdGCValue] = deferreds.map(deferred => deferred.parentNodeId)

    // As we are using `union all` as our batching mechanism there is very little gain from batching,
    // and 500 items seems to be the cutoff point where there is no more value to be had.
    val batchFutures: Vector[Future[Vector[ResolverResult[PrismaNodeWithParent]]]] = relatedModelInstanceIds
      .grouped(500)
      .toVector
      .map(ids => dataResolver.resolveByRelationManyModels(relatedField, ids, args))

    // Fetch resolver results
    val futureResolverResults: Future[Vector[ResolverResult[PrismaNodeWithParent]]] = Future
      .sequence(batchFutures)
      .map(_.flatten)

    // Assign the resolver results to each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult(
          futureResolverResults.map { resolverResults =>
            // Each deferred has exactly one ResolverResult
            val found: ResolverResult[PrismaNodeWithParent] = resolverResults.find(_.parentModelId.contains(deferred.parentNodeId)).get

            mapToConnectionOutputType(found, deferred, dataResolver.project)
          },
          order
        )
    }

    results
  }

  def mapToConnectionOutputType(input: ResolverResult[PrismaNodeWithParent], deferred: ToManyDeferred, project: Project): RelayConnectionOutputType = {
    DefaultIdBasedConnection(
      PageInfo(
        hasNextPage = input.hasNextPage,
        hasPreviousPage = input.hasPreviousPage,
        input.nodes.map(_.prismaNode).headOption.map(_.id),
        input.nodes.map(_.prismaNode).lastOption.map(_.id)
      ),
      input.nodes.map(_.prismaNode).map(x => DefaultEdge(x, x.id)),
      ConnectionParentElement(nodeId = Some(deferred.parentNodeId), field = Some(deferred.relationField), args = deferred.args)
    )
  }
}
