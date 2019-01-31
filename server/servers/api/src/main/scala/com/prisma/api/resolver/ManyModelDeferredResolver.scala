package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._

import scala.concurrent.ExecutionContext

class ManyModelDeferredResolver(resolver: DataResolver) {
  def resolve(
      orderedDeferreds: Vector[OrderedDeferred[GetNodesDeferred]]
  )(implicit ec: ExecutionContext): Vector[OrderedDeferredFutureResult[GetNodesDeferredResultType]] = {
    val deferreds             = orderedDeferreds.map(_.deferred)
    val headDeferred          = deferreds.head
    val model                 = headDeferred.model
    val args                  = headDeferred.args
    val selectedFields        = deferreds.foldLeft(SelectedFields.empty)(_ ++ _.selectedFields)
    val futureResolverResults = resolver.getNodes(model, args, selectedFields)

    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult(futureResolverResults.map(mapToConnectionOutputType(_, deferred)), order)
    }
  }

  def mapToConnectionOutputType(input: ResolverResult[PrismaNode], deferred: GetNodesDeferred): GetNodesDeferredResultType = {
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
