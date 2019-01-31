package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes.{CountNodesDeferred, OrderedDeferred, OrderedDeferredFutureResult}

class CountManyModelDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountNodesDeferred]]): Vector[OrderedDeferredFutureResult[Int]] = {
    val deferreds    = orderedDeferreds.map(_.deferred)
    val headDeferred = deferreds.head
    val countFuture  = dataResolver.countByModel(headDeferred.model, headDeferred.args)

    orderedDeferreds.map { case OrderedDeferred(_, order) => OrderedDeferredFutureResult[Int](countFuture, order) }
  }
}
