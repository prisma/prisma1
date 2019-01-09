package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes._
import sangria.execution.deferred.{Deferred, DeferredResolver}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolverImpl[CtxType](dataResolver: DataResolver) extends DeferredResolver[CtxType] {

  val oneDeferredResolver                                  = new ToOneDeferredResolver(dataResolver)
  val toOneDeferredResolver                                = new FromOneDeferredResolver(dataResolver)
  val manyModelDeferredResolver: ManyModelDeferredResolver = new ManyModelDeferredResolver(dataResolver)
  val toManyDeferredResolver: ToManyDeferredResolver       = new ToManyDeferredResolver(dataResolver)
  val countManyModelDeferredResolver                       = new CountManyModelDeferredResolver(dataResolver)
  val scalarListDeferredResolver                           = new ScalarListDeferredResolver(dataResolver)

  override def resolve(deferred: Vector[Deferred[Any]], ctx: CtxType, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {

    // group orderedDeferreds by type
    val orderedDeferred = DeferredUtils.tagDeferredByOrder(deferred)

    val toOneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ToOneDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val fromOneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: FromOneDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val manyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ManyModelDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val toManyDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ToManyDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val countManyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CountManyModelDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val scalarListDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ScalarListDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    // for every group, further break them down by their arguments
    val fromOneDeferredMap         = DeferredUtils.groupRelatedDeferred[FromOneDeferred](fromOneDeferreds)
    val manyModelDeferredsMap      = DeferredUtils.groupModelDeferred[ManyModelDeferred](manyModelDeferreds)
    val toManyDeferredsMap         = DeferredUtils.groupRelatedDeferred[ToManyDeferred](toManyDeferreds)
    val countManyModelDeferredsMap = DeferredUtils.groupModelDeferred[CountManyModelDeferred](countManyModelDeferreds)
    val scalarListDeferredsMap     = DeferredUtils.groupScalarListDeferreds(scalarListDeferreds)

    // for every group of deferreds, resolve them
    // fixme: these must be grouped first
    val toOneFutureResults = toOneDeferreds.map(deferred => oneDeferredResolver.resolve(deferred))

    val fromOneFutureResults = fromOneDeferredMap
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

    (toOneFutureResults ++
      fromOneFutureResults ++
      manyModelFutureResults ++
      toManyFutureResults ++
      countManyModelFutureResults ++
      scalarListFutureResult ++
      connectionFutureResult).sortBy(_.order).map(_.future)

  }
}
