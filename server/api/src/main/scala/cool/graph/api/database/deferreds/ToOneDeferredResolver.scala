package cool.graph.api.database.deferreds

import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.DeferredTypes.{OneDeferredResultType, OrderedDeferred, OrderedDeferredFutureResult, ToOneDeferred}
import cool.graph.shared.models.Project
import scala.concurrent.ExecutionContext.Implicits.global

class ToOneDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[ToOneDeferred]]): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // get ids of dataitems in related model we need to fetch
    val relatedModelIds = deferreds.map(_.parentNodeId).toList

    // fetch dataitems
    val futureDataItems =
      dataResolver.resolveByRelationManyModels(relatedField, relatedModelIds, args).map(_.flatMap(_.items))

    // assign the dataitem that was requested by each deferred
    val results = orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futureDataItems.map {
          dataItemsToToOneDeferredResultType(dataResolver.project, deferred, _)
        }, order)
    }

    results
  }

  private def dataItemsToToOneDeferredResultType(project: Project, deferred: ToOneDeferred, dataItems: Seq[DataItem]): Option[DataItem] = {

    def matchesRelation(dataItem: DataItem, relationSide: String) =
      dataItem.userData
        .get(relationSide)
        .flatten
        .contains(deferred.parentNodeId)

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge =
      deferred.relationField.relation.get.isSameFieldSameModelRelation(project)

    dataItems.find(
      dataItem => {
        resolveFromBothSidesAndMerge match {
          case false =>
            matchesRelation(dataItem, deferred.relationField.relationSide.get.toString)
          case true =>
            dataItem.id != deferred.parentNodeId && (matchesRelation(dataItem, deferred.relationField.relationSide.get.toString) ||
              matchesRelation(dataItem, deferred.relationField.oppositeRelationSide.get.toString))
        }
      }
    )
  }
}
