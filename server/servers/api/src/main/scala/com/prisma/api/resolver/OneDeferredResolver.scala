package com.prisma.api.resolver

import com.prisma.api.connector._
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.shared.models.Project

import scala.concurrent.ExecutionContext

class OneDeferredResolver(resolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[GetNodeDeferred]])(
      implicit ec: ExecutionContext): Vector[OrderedDeferredFutureResult[GetNodeDeferredResultType]] = {

    val deferreds      = orderedDeferreds.map(_.deferred)
    val headDeferred   = deferreds.head
    val whereField     = headDeferred.where.field
    val selectedFields = deferreds.foldLeft(SelectedFields.empty)(_ ++ _.selectedFields)

    val filter = ScalarFilter(whereField, In(orderedDeferreds.map(_.deferred.where.fieldGCValue)))

    // fetch prismaNodes
    val futurePrismaNodes = resolver.getNodes(headDeferred.where.model, QueryArguments.withFilter(filter), selectedFields).map(_.nodes)

    // assign the prismaNode that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[GetNodeDeferredResultType](futurePrismaNodes.map { nodes =>
          prismaNodesToOneDeferredResultType(resolver.project, deferred, nodes)
        }, order)
    }
  }

  private def prismaNodesToOneDeferredResultType(project: Project, deferred: GetNodeDeferred, nodes: Vector[PrismaNode]): Option[PrismaNode] = {
    def matchesWhere(prismaNode: PrismaNode) = prismaNode.data.map(deferred.where.fieldName) == deferred.where.fieldGCValue
    nodes.find(node => matchesWhere(node))
  }
}
