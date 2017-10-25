package cool.graph.client.database

import cool.graph.client.database.DeferredTypes._

import scala.concurrent.ExecutionContext.Implicits.global

class CountToManyDeferredResolver {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountToManyDeferred]], ctx: DataResolver): Vector[OrderedDeferredFutureResult[Int]] = {
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
      ctx.countByRelationManyModels(relatedField, relatedModelIds, args)

    // assign the dataitems that were requested by each deferred
    val results: Vector[OrderedDeferredFutureResult[Int]] =
      orderedDeferreds.map {
        case OrderedDeferred(deferred, order) =>
          OrderedDeferredFutureResult[Int](futureDataItems.map { counts =>
            counts.find(_._1 == deferred.parentNodeId).map(_._2).get
          }, order)
      }

    results
  }

}
