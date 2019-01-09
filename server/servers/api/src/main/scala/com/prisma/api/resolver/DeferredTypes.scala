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

  trait ModelDeferred[+T] extends Deferred[T] {
    def model: Model
    def args: QueryArguments
  }

  case class ManyModelDeferred(
      model: Model,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends ModelDeferred[RelayConnectionOutputType]

  case class ToOneDeferred(
      model: Model,
      where: NodeSelector
  ) extends Deferred[OneDeferredResultType]

  case class CountManyModelDeferred(
      model: Model,
      args: QueryArguments
  ) extends ModelDeferred[Int]

  trait RelationDeferred[+T] extends Deferred[T] {
    def relationField: RelationField
    def parentNodeId: IdGCValue
    def args: QueryArguments
  }

  case class FromOneDeferred(
      relationField: RelationField,
      parentNodeId: IdGCValue,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends RelationDeferred[OneDeferredResultType]

  case class ToManyDeferred(
      relationField: RelationField,
      parentNodeId: IdGCValue,
      args: QueryArguments,
      selectedFields: SelectedFields
  ) extends RelationDeferred[RelayConnectionOutputType]

  type OneDeferredResultType        = Option[PrismaNode]
  type RelayConnectionOutputType    = IdBasedConnection[PrismaNode]
  type ScalarListDeferredResultType = Vector[Any]

  case class ScalarListDeferred(model: Model, field: ScalarField, nodeId: IdGCValue) extends Deferred[ScalarListDeferredResultType]

  case class IdBasedConnectionDeferred(conn: DefaultIdBasedConnection[PrismaNode]) extends Deferred[RelayConnectionOutputType]
}
