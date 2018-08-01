package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes.{CountManyModelDeferred, OrderedDeferred, OrderedDeferredFutureResult}

class CountManyModelDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountManyModelDeferred]]): Vector[OrderedDeferredFutureResult[Int]] = {
    val deferreds    = orderedDeferreds.map(_.deferred)
    val headDeferred = deferreds.head
    val whereFilter  = headDeferred.args.flatMap(_.filter)
    val countFuture  = dataResolver.countByModel(headDeferred.model, whereFilter)

    orderedDeferreds.map { case OrderedDeferred(_, order) => OrderedDeferredFutureResult[Int](countFuture, order) }
  }
}
