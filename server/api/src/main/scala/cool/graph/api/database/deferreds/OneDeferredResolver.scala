package cool.graph.api.database.deferreds

import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.DeferredTypes.{OneDeferred, OneDeferredResultType, OrderedDeferred, OrderedDeferredFutureResult}
import cool.graph.shared.models.Project
import scala.concurrent.ExecutionContext.Implicits.global

class OneDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[OneDeferred]]): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfOneDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head

    // fetch dataitems
    val futureDataItems =
      dataResolver.batchResolveByUnique(headDeferred.model, headDeferred.key, deferreds.map(_.value).toList)

    // assign the dataitem that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futureDataItems.map {
          dataItemsToToOneDeferredResultType(dataResolver.project, deferred, _)
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
