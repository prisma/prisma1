package com.prisma.api.resolver

import com.prisma.api.connector.{NodeSelector, PrismaNode, QueryArguments}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Field, Model}
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

  case class ManyModelDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[RelayConnectionOutputType]

  case class ManyModelExistsDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[Boolean]

  case class CountManyModelDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[Int]

  trait RelatedArgs {
    def relationField: Field
    def parentNodeId: IdGCValue
    def args: Option[QueryArguments]
  }

  trait RelationDeferred[+T] extends RelatedArgs with Deferred[T] {
    def relationField: Field
    def parentNodeId: IdGCValue
    def args: Option[QueryArguments]
  }

  type OneDeferredResultType = Option[PrismaNode]

  case class OneDeferred(model: Model, where: NodeSelector)                                             extends Deferred[OneDeferredResultType] {}
  case class ToOneDeferred(relationField: Field, parentNodeId: IdGCValue, args: Option[QueryArguments]) extends RelationDeferred[OneDeferredResultType]

  case class ToManyDeferred(relationField: Field, parentNodeId: IdGCValue, args: Option[QueryArguments]) extends RelationDeferred[RelayConnectionOutputType]

  case class CountToManyDeferred(relationField: Field, parentNodeId: IdGCValue, args: Option[QueryArguments]) extends RelationDeferred[Int]

  type SimpleConnectionOutputType   = Seq[PrismaNode]
  type RelayConnectionOutputType    = IdBasedConnection[PrismaNode]
  type ScalarListDeferredResultType = Vector[Any]

  case class ScalarListDeferred(model: Model, field: Field, nodeId: IdGCValue) extends Deferred[ScalarListDeferredResultType]
}
