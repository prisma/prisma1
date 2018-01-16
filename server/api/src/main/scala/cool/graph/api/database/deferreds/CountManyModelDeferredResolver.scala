package cool.graph.api.database.deferreds

import cool.graph.api.database.DataResolver
import cool.graph.api.database.DeferredTypes.{CountManyModelDeferred, OrderedDeferred, OrderedDeferredFutureResult}

class CountManyModelDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountManyModelDeferred]]): Vector[OrderedDeferredFutureResult[Int]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val model        = headDeferred.model
    val args         = headDeferred.args
    val where        = args.flatMap(_.filter)

    val futureDataItems = dataResolver.countByModel(model, where)

    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Int](futureDataItems, order)
    }

    results
  }
}
