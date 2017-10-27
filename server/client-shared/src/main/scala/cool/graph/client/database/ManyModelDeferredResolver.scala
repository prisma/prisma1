package cool.graph.client.database

import cool.graph.client.database.DeferredTypes._

import scala.concurrent.ExecutionContext.Implicits.global

abstract class ManyModelDeferredResolver[ConnectionOutputType] {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ManyModelDeferred[ConnectionOutputType]]],
              resolver: DataResolver): Vector[OrderedDeferredFutureResult[ConnectionOutputType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred          = deferreds.head
    val model                 = headDeferred.model
    val args                  = headDeferred.args
    val futureResolverResults = resolver.resolveByModel(model, args)

    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ConnectionOutputType](futureResolverResults.map(mapToConnectionOutputType(_, deferred)), order)
    }

    results
  }

  def mapToConnectionOutputType(input: ResolverResult, deferred: ManyModelDeferred[ConnectionOutputType]): ConnectionOutputType
}

class SimpleManyModelDeferredResolver extends ManyModelDeferredResolver[SimpleConnectionOutputType] {
  def mapToConnectionOutputType(input: ResolverResult, deferred: ManyModelDeferred[SimpleConnectionOutputType]): SimpleConnectionOutputType =
    input.items.toList
}

class RelayManyModelDeferredResolver extends ManyModelDeferredResolver[RelayConnectionOutputType] {
  def mapToConnectionOutputType(input: ResolverResult, deferred: ManyModelDeferred[RelayConnectionOutputType]): RelayConnectionOutputType = {
    DefaultIdBasedConnection(
      PageInfo(
        hasNextPage = input.hasNextPage,
        hasPreviousPage = input.hasPreviousPage,
        input.items.headOption.map(_.id),
        input.items.lastOption.map(_.id)
      ),
      input.items.map(x => DefaultEdge(x, x.id)),
      ConnectionParentElement(None, None, deferred.args)
    )
  }
}
