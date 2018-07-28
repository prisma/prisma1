package com.prisma.api.resolver

import com.prisma.api.connector.{NodeSelector, PrismaNode, QueryArguments}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Model, RelationField, ScalarField}
import sangria.execution.deferred.Deferred

import scala.concurrent.Future

object DeferredTypes {

  trait Ordered {
    def order: Int
  }

  case class OrderedDeferred[T](deferred: T, order: Int)                                     extends Ordered
  case class OrderedDeferredFutureResult[ResultType](future: Future[ResultType], order: Int) extends Ordered

  trait ModelArgs {
    def model: Model
    def args: Option[QueryArguments]
  }

  trait ModelDeferred[+T] extends ModelArgs with Deferred[T] {
    model: Model
    args: Option[QueryArguments]
  }

  case class ManyModelDeferred(model: Model, args: Option[QueryArguments])      extends ModelDeferred[RelayConnectionOutputType]
  case class CountManyModelDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[Int]

  trait RelatedArgs {
    def relationField: RelationField
    def parentNodeId: IdGCValue
    def args: Option[QueryArguments]
  }

  trait RelationDeferred[+T] extends RelatedArgs with Deferred[T] {
    def relationField: RelationField
    def parentNodeId: IdGCValue
    def args: Option[QueryArguments]
  }

  type OneDeferredResultType = Option[PrismaNode]

  case class OneDeferred(model: Model, where: NodeSelector)                                                     extends Deferred[OneDeferredResultType]
  case class ToOneDeferred(relationField: RelationField, parentNodeId: IdGCValue, args: Option[QueryArguments]) extends RelationDeferred[OneDeferredResultType]
  case class ToManyDeferred(relationField: RelationField, parentNodeId: IdGCValue, args: Option[QueryArguments])
      extends RelationDeferred[RelayConnectionOutputType]

  type SimpleConnectionOutputType   = Seq[PrismaNode]
  type RelayConnectionOutputType    = IdBasedConnection[PrismaNode]
  type ScalarListDeferredResultType = Vector[Any]

  case class ScalarListDeferred(model: Model, field: ScalarField, nodeId: IdGCValue) extends Deferred[ScalarListDeferredResultType]

  case class IdBasedConnectionDeferred(conn: DefaultIdBasedConnection[PrismaNode]) extends Deferred[RelayConnectionOutputType]
}
