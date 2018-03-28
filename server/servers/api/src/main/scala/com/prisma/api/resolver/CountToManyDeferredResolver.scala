package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes.{CountToManyDeferred, OrderedDeferred, OrderedDeferredFutureResult}
import com.prisma.gc_values.GraphQLIdGCValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CountToManyDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[CountToManyDeferred]]): Vector[OrderedDeferredFutureResult[Int]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfRelatedDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    // get ids of dataitems in related model we need to fetch
    val relatedModelIds = deferreds.map(deferred => GraphQLIdGCValue(deferred.parentNodeId))

    // fetch dataitems
    val futureNodeCounts: Future[Vector[(GraphQLIdGCValue, Int)]] = dataResolver.countByRelationManyModels(relatedField, relatedModelIds, args)

    // assign the dataitems that were requested by each deferred

    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Int](futureNodeCounts.map { counts =>
          counts.find(_._1.value == deferred.parentNodeId).map(_._2).get
        }, order)
    }
  }

}
