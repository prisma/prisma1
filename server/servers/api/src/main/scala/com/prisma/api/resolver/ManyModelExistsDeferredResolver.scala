package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes.{ManyModelExistsDeferred, OrderedDeferred, OrderedDeferredFutureResult}

import scala.concurrent.{ExecutionContext, Future}

class ManyModelExistsDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ManyModelExistsDeferred]],
              executionContext: ExecutionContext): Vector[OrderedDeferredFutureResult[Boolean]] = {
    implicit val ec: ExecutionContext = executionContext

    val deferreds = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val model        = headDeferred.model
    val args         = headDeferred.args

    // all deferred have the same return value
    val futurePrismaNodes = Future.successful(dataResolver.resolveByModel(model, args))

    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Boolean](futurePrismaNodes.flatMap(identity).map(_.nodes.nonEmpty), order)
    }

    results
  }
}
