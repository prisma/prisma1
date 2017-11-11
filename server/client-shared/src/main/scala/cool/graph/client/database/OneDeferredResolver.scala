package cool.graph.client.database

import cool.graph.DataItem
import cool.graph.client.database.DeferredTypes._
import cool.graph.shared.models.Project

import scala.concurrent.ExecutionContext.Implicits.global

class OneDeferredResolver {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[OneDeferred]], ctx: DataResolver): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfOneDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head

    // fetch dataitems
    val futureDataItems =
      ctx.batchResolveByUnique(headDeferred.model, headDeferred.key, deferreds.map(_.value).toList)

    // assign the dataitem that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futureDataItems.map {
          dataItemsToToOneDeferredResultType(ctx.project, deferred, _)
        }, order)
    }

    results
  }

  private def dataItemsToToOneDeferredResultType(project: Project, deferred: OneDeferred, dataItems: Seq[DataItem]): Option[DataItem] = {

    deferred.key match {
      case "id" => dataItems.find(_.id == deferred.value)
      case _ =>
        dataItems.find(_.getOption(deferred.key) == Some(deferred.value))
    }
  }
}
