package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, ScalarListValues}
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.gc_values.GCValueExtractor

import scala.concurrent.{ExecutionContext, Future}

class ScalarListDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ScalarListDeferred]],
              executionContext: ExecutionContext): Vector[OrderedDeferredFutureResult[ScalarListDeferredResultType]] = {
    implicit val ec: ExecutionContext = executionContext
    val deferreds                     = orderedDeferreds.map(_.deferred)
    val extractor                     = GCValueExtractor

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfScalarListDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val deferredIds  = deferreds.map(deferred => deferred.nodeId)

    val futureValues: Future[Vector[ScalarListValues]] = dataResolver.batchResolveScalarList(headDeferred.model, headDeferred.field, deferredIds)

    // assign and sort the scalarListValues that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ScalarListDeferredResultType](
          futureValues.map { values =>
            values.filter(_.nodeId == deferred.nodeId).flatMap(values => extractor.fromListGCValue(values.value))
          },
          order
        )
    }
  }
}
