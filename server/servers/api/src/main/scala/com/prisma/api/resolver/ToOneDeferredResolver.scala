package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._

import scala.concurrent.ExecutionContext

class ToOneDeferredResolver(resolver: DataResolver) {
  def resolve(
      deferred: OrderedDeferred[ToOneDeferred]
  )(implicit ec: ExecutionContext): OrderedDeferredFutureResult[OneDeferredResultType] = {
    val model                 = deferred.deferred.model
    val where                 = deferred.deferred.where
    val futureResolverResults = resolver.getNodeByWhere(where, SelectedFields.all(model))

    OrderedDeferredFutureResult(futureResolverResults, deferred.order)

  }
}
