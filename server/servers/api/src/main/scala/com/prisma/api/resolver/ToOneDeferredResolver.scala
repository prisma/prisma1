package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._

import scala.concurrent.ExecutionContext

class ToOneDeferredResolver(resolver: DataResolver) {
  def resolve(
      deferred: OrderedDeferred[ToOneDeferred]
  )(implicit ec: ExecutionContext): OrderedDeferredFutureResult[OneDeferredResultType] = {
    val where                 = deferred.deferred.where
    val futureResolverResults = resolver.getNodeByWhere(where, deferred.deferred.selectedFields)

    OrderedDeferredFutureResult(futureResolverResults, deferred.order)
  }
}
