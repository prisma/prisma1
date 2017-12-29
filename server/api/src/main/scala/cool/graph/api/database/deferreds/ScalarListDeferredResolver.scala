package cool.graph.api.database.deferreds

import cool.graph.api.database.DeferredTypes._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.shared.models.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScalarListDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ScalarListDeferred]]): Vector[OrderedDeferredFutureResult[ScalarListDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfScalarListDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head

    // fetch dataitems
    val futureValues: Future[Vector[Any]] =
      dataResolver.resolveScalarList(headDeferred.model, headDeferred.field)

    // assign the dataitem that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ScalarListDeferredResultType](futureValues, order)
    }

    results
  }
}
