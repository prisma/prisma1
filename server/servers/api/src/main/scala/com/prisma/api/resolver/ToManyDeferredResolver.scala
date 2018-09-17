package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.gc_values.IdGCValue
import com.prisma.tracing.Tracing

import scala.concurrent.{ExecutionContext, Future}

class ToManyDeferredResolver(dataResolver: DataResolver) extends Tracing {
  def resolve(
      orderedDeferreds: Vector[OrderedDeferred[ToManyDeferred]]
  )(implicit ec: ExecutionContext): Vector[OrderedDeferredFutureResult[RelayConnectionOutputType]] = {
    val deferreds      = orderedDeferreds.map(_.deferred)
    val headDeferred   = deferreds.head
    val relatedField   = headDeferred.relationField
    val args           = headDeferred.args
    val selectedFields = deferreds.foldLeft(SelectedFields.empty)(_ ++ _.selectedFields)

    // Get ids of nodes in related model we need to fetch (actual rows of data)
    val relatedModelInstanceIds: Vector[IdGCValue] = deferreds.map(deferred => deferred.parentNodeId)

    // As we are using `union all` as our batching mechanism there is very little gain from batching,
    // and 500 items seems to be the cutoff point where there is no more value to be had.
    // todo figure out the correct group size see to not run into parameter limits see:
    // https://stackoverflow.com/questions/6581573/what-are-the-max-number-of-allowable-parameters-per-database-provider-type
    val batchFutures: Vector[Future[Vector[ResolverResult[PrismaNodeWithParent]]]] = relatedModelInstanceIds.distinct
      .grouped(500)
      .toVector
      .map(ids => dataResolver.getRelatedNodes(relatedField, ids, args, selectedFields))

    // Fetch resolver results
    val futureResolverResults: Future[Vector[ResolverResult[PrismaNodeWithParent]]] = Future
      .sequence(batchFutures)
      .map(_.flatten)

    // Assign the resolver results to each deferred

    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult(
          futureResolverResults.map { resolverResults =>
            // Each deferred has exactly one ResolverResult
            val found: ResolverResult[PrismaNodeWithParent] = resolverResults.find(_.parentModelId.contains(deferred.parentNodeId)).get

            mapToConnectionOutputType(found, deferred)
          },
          order
        )
    }
  }

  private def mapToConnectionOutputType(input: ResolverResult[PrismaNodeWithParent], deferred: ToManyDeferred): RelayConnectionOutputType = {
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
