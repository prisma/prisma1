package cool.graph.client.database

import cool.graph.DataItem
import cool.graph.shared.models.{AuthenticatedRequest, Field, Model}
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

  case class ManyModelDeferred[ConnectionOutputType](model: Model, args: Option[QueryArguments]) extends ModelDeferred[ConnectionOutputType]

  case class ManyModelExistsDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[Boolean]

  case class CountManyModelDeferred(model: Model, args: Option[QueryArguments]) extends ModelDeferred[Int]

  trait RelatedArgs {
    def relationField: Field
    def parentNodeId: String
    def args: Option[QueryArguments]
  }

  trait RelationDeferred[+T] extends RelatedArgs with Deferred[T] {
    def relationField: Field
    def parentNodeId: String
    def args: Option[QueryArguments]
  }

  type OneDeferredResultType = Option[DataItem]
  case class OneDeferred(model: Model, key: String, value: Any)                                      extends Deferred[OneDeferredResultType]
  case class ToOneDeferred(relationField: Field, parentNodeId: String, args: Option[QueryArguments]) extends RelationDeferred[OneDeferredResultType]

  case class ToManyDeferred[ConnectionOutputType](relationField: Field, parentNodeId: String, args: Option[QueryArguments])
      extends RelationDeferred[ConnectionOutputType]

  case class CountToManyDeferred(relationField: Field, parentNodeId: String, args: Option[QueryArguments]) extends RelationDeferred[Int]

  type SimpleConnectionOutputType = Seq[DataItem]
  type RelayConnectionOutputType  = IdBasedConnection[DataItem]

  case class CheckPermissionDeferred(model: Model,
                                     field: Field,
                                     nodeId: String,
                                     authenticatedRequest: Option[AuthenticatedRequest],
                                     value: Any,
                                     node: DataItem,
                                     alwaysQueryMasterDatabase: Boolean)
      extends Deferred[Boolean]
}
