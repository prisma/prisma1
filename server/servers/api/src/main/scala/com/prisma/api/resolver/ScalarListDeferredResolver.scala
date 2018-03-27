package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, ScalarListValue}
import com.prisma.api.resolver.DeferredTypes._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScalarListDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ScalarListDeferred]]): Vector[OrderedDeferredFutureResult[ScalarListDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfScalarListDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head

    val futureValues: Future[Vector[ScalarListValue]] = dataResolver.batchResolveScalarList(headDeferred.model, headDeferred.field, deferreds.map(_.nodeId))

    // assign and sort the scalarListValues that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ScalarListDeferredResultType](futureValues.map {
          _.filter(_.nodeId == deferred.nodeId).sortBy(_.position).map(_.value)
        }, order)
    }

    results
  }
}
