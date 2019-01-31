package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes._
import sangria.execution.deferred.{Deferred, DeferredResolver}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolverImpl[CtxType](dataResolver: DataResolver) extends DeferredResolver[CtxType] {

  val oneDeferredResolver            = new OneDeferredResolver(dataResolver)
  val toOneDeferredResolver          = new ToOneDeferredResolver(dataResolver)
  val manyModelDeferredResolver      = new ManyModelDeferredResolver(dataResolver)
  val toManyDeferredResolver         = new ToManyDeferredResolver(dataResolver)
  val countManyModelDeferredResolver = new CountManyModelDeferredResolver(dataResolver)
  val scalarListDeferredResolver     = new ScalarListDeferredResolver(dataResolver)

  override def resolve(deferred: Vector[Deferred[Any]], ctx: CtxType, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {

    // group orderedDeferreds by type
    val orderedDeferred = DeferredUtils.tagDeferredByOrder(deferred)

    val oneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodeDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val toOneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodeByParentDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val manyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodesDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val toManyDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodesByParentDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val countManyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CountNodesDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val scalarListDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ScalarListDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    // for every group, further break them down by their arguments
    val oneDeferredMap             = DeferredUtils.groupGetNodeDeferreds(oneDeferreds)
    val toOneDeferredMap           = DeferredUtils.groupRelatedDeferred[GetNodeByParentDeferred](toOneDeferreds)
    val manyModelDeferredsMap      = DeferredUtils.groupGetNodesDeferreds[GetNodesDeferred](manyModelDeferreds)
    val toManyDeferredsMap         = DeferredUtils.groupRelatedDeferred[GetNodesByParentDeferred](toManyDeferreds)
    val countManyModelDeferredsMap = DeferredUtils.groupGetNodesDeferreds[CountNodesDeferred](countManyModelDeferreds)
    val scalarListDeferredsMap     = DeferredUtils.groupScalarListDeferreds(scalarListDeferreds)

//    val oneFutureResults = oneDeferreds.map(deferred => oneDeferredResolver.resolve(deferred))

    val oneFutureResults = oneDeferredMap
      .map {
        case (_, value) => oneDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val toOneFutureResults = toOneDeferredMap
      .map {
        case (_, value) => toOneDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val manyModelFutureResults = manyModelDeferredsMap
      .map {
        case (_, value) => manyModelDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val toManyFutureResults = toManyDeferredsMap
      .map {
        case (_, value) => toManyDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val countManyModelFutureResults = countManyModelDeferredsMap
      .map {
        case (_, value) => countManyModelDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val scalarListFutureResult = scalarListDeferredsMap
      .map {
        case (_, value) => scalarListDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val connectionFutureResult = orderedDeferred.collect {
      case OrderedDeferred(deferred: IdBasedConnectionDeferred, order) =>
        OrderedDeferredFutureResult(Future.successful(deferred.conn), order)
    }

    (oneFutureResults ++
      toOneFutureResults ++
      manyModelFutureResults ++
      toManyFutureResults ++
      countManyModelFutureResults ++
      scalarListFutureResult ++
      connectionFutureResult).sortBy(_.order).map(_.future)

  }
}
