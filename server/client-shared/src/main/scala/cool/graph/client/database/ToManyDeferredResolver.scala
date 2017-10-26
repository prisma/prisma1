package cool.graph.client.database

import cool.graph.client.database.DeferredTypes.{ToManyDeferred, _}
import cool.graph.shared.models.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class ToManyDeferredResolver[ConnectionOutputType] {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ToManyDeferred[ConnectionOutputType]]],
              ctx: DataResolver): Vector[OrderedDeferredFutureResult[ConnectionOutputType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // Check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // Get ids of nodes in related model we need to fetch (actual rows of data)
    val relatedModelInstanceIds = deferreds.map(_.parentNodeId).toList

    // As we are using `union all` as our batching mechanism there is very little gain from batching,
    // and 500 items seems to be the cutoff point where there is no more value to be had.
    val batchFutures: Seq[Future[Seq[ResolverResult]]] = relatedModelInstanceIds
      .grouped(500)
      .toList
      .map(ctx.resolveByRelationManyModels(relatedField, _, args))

    // Fetch resolver results
    val futureResolverResults: Future[Seq[ResolverResult]] = Future
      .sequence(batchFutures)
      .map(_.flatten)

    // Assign the resolver results to each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ConnectionOutputType](
          futureResolverResults.map { resolverResults =>
            // Each deferred has exactly one ResolverResult
            mapToConnectionOutputType(resolverResults.find(_.parentModelId.contains(deferred.parentNodeId)).get, deferred, ctx.project)
          },
          order
        )
    }

    results
  }

  def mapToConnectionOutputType(input: ResolverResult, deferred: ToManyDeferred[ConnectionOutputType], project: Project): ConnectionOutputType
}

class SimpleToManyDeferredResolver extends ToManyDeferredResolver[SimpleConnectionOutputType] {
  override def mapToConnectionOutputType(input: ResolverResult,
                                         deferred: ToManyDeferred[SimpleConnectionOutputType],
                                         project: Project): SimpleConnectionOutputType = input.items.toList
}

class RelayToManyDeferredResolver extends ToManyDeferredResolver[RelayConnectionOutputType] {
  def mapToConnectionOutputType(input: ResolverResult, deferred: ToManyDeferred[RelayConnectionOutputType], project: Project): RelayConnectionOutputType = {
    DefaultIdBasedConnection(
      PageInfo(
        hasNextPage = input.hasNextPage,
        hasPreviousPage = input.hasPreviousPage,
        input.items.headOption.map(_.id),
        input.items.lastOption.map(_.id)
      ),
      input.items.map(x => DefaultEdge(x, x.id)),
      ConnectionParentElement(nodeId = Some(deferred.parentNodeId), field = Some(deferred.relationField), args = deferred.args)
    )
  }
}
