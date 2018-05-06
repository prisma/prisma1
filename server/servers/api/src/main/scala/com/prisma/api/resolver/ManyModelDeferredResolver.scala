package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._

import scala.concurrent.ExecutionContext

class ManyModelDeferredResolver(resolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ManyModelDeferred]],
              executionContext: ExecutionContext): Vector[OrderedDeferredFutureResult[RelayConnectionOutputType]] = {
    implicit val ec: ExecutionContext = executionContext
    val deferreds                     = orderedDeferreds.map(_.deferred)

    DeferredUtils.checkSimilarityOfModelDeferredsAndThrow(deferreds)

    val headDeferred                 = deferreds.head
    val model                        = headDeferred.model
    val args: Option[QueryArguments] = headDeferred.args
    val futureResolverResults        = resolver.resolveByModel(model, args)

    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult(futureResolverResults.map(mapToConnectionOutputType(_, deferred)), order)
    }
  }

  def mapToConnectionOutputType(input: ResolverResult[PrismaNode], deferred: ManyModelDeferred): RelayConnectionOutputType = {
    DefaultIdBasedConnection(
      PageInfo(
        hasNextPage = input.hasNextPage,
        hasPreviousPage = input.hasPreviousPage,
        input.nodes.headOption.map(_.id),
        input.nodes.lastOption.map(_.id)
      ),
      input.nodes.map(x => DefaultEdge(x, x.id)),
      ConnectionParentElement(None, None, deferred.args)
    )
  }
}
