package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, ScalarListValues}
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.gc_values.IdGCValue
import com.prisma.util.gc_value.GCValueExtractor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScalarListDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ScalarListDeferred]]): Vector[OrderedDeferredFutureResult[ScalarListDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)
    val extractor = GCValueExtractor

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfScalarListDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val deferredIds  = deferreds.map(deferred => IdGCValue(deferred.nodeId))

    val futureValues: Future[Vector[ScalarListValues]] = dataResolver.batchResolveScalarList(headDeferred.model, headDeferred.field, deferredIds)

    // assign and sort the scalarListValues that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[ScalarListDeferredResultType](
          futureValues.map { values =>
            values.filter(_.nodeId == IdGCValue(deferred.nodeId)).flatMap(values => extractor.fromListGCValue(values.value))
          },
          order
        )
    }
  }
}
