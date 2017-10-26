package cool.graph.client.database

import cool.graph.client.database.DeferredTypes._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManyModelExistsDeferredResolver {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ManyModelExistsDeferred]], ctx: DataResolver): Vector[OrderedDeferredFutureResult[Boolean]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val model        = headDeferred.model
    val args         = headDeferred.args

    // all deferred have the same return value
    val futureDataItems = Future.successful(ctx.resolveByModel(model, args))

    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Boolean](futureDataItems.flatMap(identity).map(_.items.nonEmpty), order)
    }

    results
  }
}
