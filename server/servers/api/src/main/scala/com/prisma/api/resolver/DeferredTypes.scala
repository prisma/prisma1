package com.prisma.api.resolver

import com.prisma.api.connector.{NodeSelector, PrismaNode, QueryArguments, SelectedFields}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Model, RelationField, ScalarField}
import sangria.execution.deferred.Deferred

import scala.concurrent.Future

object DeferredTypes {

  trait Ordered {
    def order: Int
  }

  case class OrderedDeferred[T](deferred: T, order: Int)                   extends Ordered
  case class OrderedDeferredFutureResult[T](future: Future[T], order: Int) extends Ordered

  trait NodeDeferred[+T] extends Deferred[T] {
    def model: Model
  }
  trait NodeWithArgsDeferred[+T] extends NodeDeferred[T] {
    def args: QueryArguments
  }

  case class GetNodesDeferred(
      model: Model,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends NodeWithArgsDeferred[GetNodesDeferredResultType]

  case class GetNodeDeferred(
      model: Model,
      where: NodeSelector,
      selectedFields: SelectedFields
  ) extends NodeDeferred[GetNodeDeferredResultType]

  case class CountNodesDeferred(
      model: Model,
      args: QueryArguments
  ) extends NodeWithArgsDeferred[Int]

  trait NodeByParentDeferred[+T] extends Deferred[T] {
    def parentField: RelationField
    def parentNodeId: IdGCValue
    def args: QueryArguments
  }

  case class GetNodeByParentDeferred(
      parentField: RelationField,
      parentNodeId: IdGCValue,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends NodeByParentDeferred[GetNodeDeferredResultType]

  case class GetNodesByParentDeferred(
      parentField: RelationField,
      parentNodeId: IdGCValue,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends NodeByParentDeferred[GetNodesDeferredResultType]

  type GetNodeDeferredResultType    = Option[PrismaNode]
  type GetNodesDeferredResultType   = IdBasedConnection[PrismaNode]
  type ScalarListDeferredResultType = Vector[Any]

  case class ScalarListDeferred(model: Model, field: ScalarField, nodeId: IdGCValue) extends Deferred[ScalarListDeferredResultType]

  case class IdBasedConnectionDeferred(conn: DefaultIdBasedConnection[PrismaNode]) extends Deferred[GetNodesDeferredResultType]
}
