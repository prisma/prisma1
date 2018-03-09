package com.prisma.api.database.deferreds

import com.prisma.api.database.DataResolver
import com.prisma.api.database.DeferredTypes._
import com.prisma.api.schema.ApiUserContext
import sangria.execution.deferred.{Deferred, DeferredResolver}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class DeferredResolverProvider[CtxType](dataResolver: DataResolver) extends DeferredResolver[CtxType] {

  val toManyDeferredResolver: ToManyDeferredResolver       = new ToManyDeferredResolver(dataResolver)
  val manyModelDeferredResolver: ManyModelDeferredResolver = new ManyModelDeferredResolver(dataResolver)
  val manyModelsExistsDeferredResolver                     = new ManyModelExistsDeferredResolver(dataResolver)
  val countManyModelDeferredResolver                       = new CountManyModelDeferredResolver(dataResolver)
  val countToManyDeferredResolver                          = new CountToManyDeferredResolver(dataResolver)
  val toOneDeferredResolver                                = new ToOneDeferredResolver(dataResolver)
  val oneDeferredResolver                                  = new OneDeferredResolver(dataResolver)
  val scalarListDeferredResolver                           = new ScalarListDeferredResolver(dataResolver)

  override def resolve(deferred: Vector[Deferred[Any]], ctx: CtxType, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {

    // group orderedDeferreds by type
    val orderedDeferred = DeferredUtils.tagDeferredByOrder(deferred)

    val manyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ManyModelDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val manyModelExistsDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ManyModelExistsDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val countManyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CountManyModelDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val toManyDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ToManyDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val countToManyDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CountToManyDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val toOneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ToOneDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val oneDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: OneDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val scalarListDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ScalarListDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val checkScalarFieldPermissionsDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CheckPermissionDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    // for every group, further break them down by their arguments
    val manyModelDeferredsMap = DeferredUtils.groupModelDeferred[ManyModelDeferred](manyModelDeferreds)

    val manyModelExistsDeferredsMap = DeferredUtils.groupModelExistsDeferred[ManyModelExistsDeferred](manyModelExistsDeferreds)

    val countManyModelDeferredsMap = DeferredUtils.groupModelDeferred[CountManyModelDeferred](countManyModelDeferreds)

    val toManyDeferredsMap = DeferredUtils.groupRelatedDeferred[ToManyDeferred](toManyDeferreds)

    val countToManyDeferredsMap = DeferredUtils.groupRelatedDeferred[CountToManyDeferred](countToManyDeferreds)

    val toOneDeferredMap = DeferredUtils.groupRelatedDeferred[ToOneDeferred](toOneDeferreds)

    val oneDeferredsMap = DeferredUtils.groupOneDeferred(oneDeferreds)

    val scalarListDeferredsMap = DeferredUtils.groupScalarListDeferreds(scalarListDeferreds)

    // for every group of deferreds, resolve them
    val manyModelFutureResults = manyModelDeferredsMap
      .map {
        case (key, value) =>
          manyModelDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val manyModelExistsFutureResults = manyModelExistsDeferredsMap
      .map {
        case (key, value) =>
          manyModelsExistsDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val countManyModelFutureResults = countManyModelDeferredsMap
      .map {
        case (key, value) =>
          countManyModelDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val toManyFutureResults = toManyDeferredsMap
      .map {
        case (key, value) =>
          toManyDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val countToManyFutureResults = countToManyDeferredsMap
      .map {
        case (key, value) =>
          countToManyDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val toOneFutureResults = toOneDeferredMap
      .map {
        case (key, value) =>
          toOneDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val oneFutureResult = oneDeferredsMap
      .map {
        case (key, value) =>
          oneDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val scalarListFutureResult = scalarListDeferredsMap
      .map {
        case (field, value) => scalarListDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    (manyModelFutureResults ++
      manyModelExistsFutureResults ++
      countManyModelFutureResults ++
      toManyFutureResults ++
      countToManyFutureResults ++
      toOneFutureResults ++
      oneFutureResult ++
      scalarListFutureResult).sortBy(_.order).map(_.future)
  }
}
