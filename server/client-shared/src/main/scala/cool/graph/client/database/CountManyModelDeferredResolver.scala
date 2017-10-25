package cool.graph.client.database

import cool.graph.client.database.DeferredTypes._

class CountManyModelDeferredResolver {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountManyModelDeferred]], ctx: DataResolver): Vector[OrderedDeferredFutureResult[Int]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val model        = headDeferred.model
    val args         = headDeferred.args

    val futureDataItems = ctx.countByModel(model, args)

    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Int](futureDataItems, order)
    }

    results
  }
}
