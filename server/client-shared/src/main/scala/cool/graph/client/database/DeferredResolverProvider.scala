package cool.graph.client.database

import cool.graph.client.database.DeferredTypes._
import sangria.execution.deferred.{Deferred, DeferredResolver}
import scaldi.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class DeferredResolverProvider[ConnectionOutputType, Context <: { def dataResolver: DataResolver }](
    toManyDeferredResolver: ToManyDeferredResolver[ConnectionOutputType],
    manyModelDeferredResolver: ManyModelDeferredResolver[ConnectionOutputType],
    skipPermissionCheck: Boolean = false)(implicit inj: Injector)
    extends DeferredResolver[Context] {

  override def resolve(deferred: Vector[Deferred[Any]], ctx: Context, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {

    val checkScalarFieldPermissionsDeferredResolver =
      new CheckScalarFieldPermissionsDeferredResolver(skipPermissionCheck = skipPermissionCheck, ctx.dataResolver.project)

    // group orderedDeferreds by type
    val orderedDeferred = DeferredUtils.tagDeferredByOrder(deferred)

    val manyModelDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ManyModelDeferred[ConnectionOutputType], order) =>
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
      case OrderedDeferred(deferred: ToManyDeferred[ConnectionOutputType], order) =>
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

    val checkScalarFieldPermissionsDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CheckPermissionDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    // for every group, further break them down by their arguments
    val manyModelDeferredsMap = DeferredUtils
      .groupModelDeferred[ManyModelDeferred[ConnectionOutputType]](manyModelDeferreds)

    val manyModelExistsDeferredsMap = DeferredUtils
      .groupModelExistsDeferred[ManyModelExistsDeferred](manyModelExistsDeferreds)

    val countManyModelDeferredsMap = DeferredUtils
      .groupModelDeferred[CountManyModelDeferred](countManyModelDeferreds)

    val toManyDeferredsMap =
      DeferredUtils.groupRelatedDeferred[ToManyDeferred[ConnectionOutputType]](toManyDeferreds)

    val countToManyDeferredsMap =
      DeferredUtils.groupRelatedDeferred[CountToManyDeferred](countToManyDeferreds)

    val toOneDeferredMap =
      DeferredUtils.groupRelatedDeferred[ToOneDeferred](toOneDeferreds)

    val oneDeferredsMap = DeferredUtils.groupOneDeferred(oneDeferreds)

    val checkScalarFieldPermissionsDeferredsMap =
      DeferredUtils.groupPermissionDeferred(checkScalarFieldPermissionsDeferreds)

    // for every group of deferreds, resolve them
    val manyModelFutureResults = manyModelDeferredsMap
      .map {
        case (key, value) =>
          manyModelDeferredResolver.resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val manyModelExistsFutureResults = manyModelExistsDeferredsMap
      .map {
        case (key, value) =>
          new ManyModelExistsDeferredResolver().resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val countManyModelFutureResults = countManyModelDeferredsMap
      .map {
        case (key, value) =>
          new CountManyModelDeferredResolver().resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val toManyFutureResults = toManyDeferredsMap
      .map {
        case (key, value) =>
          toManyDeferredResolver.resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val countToManyFutureResults = countToManyDeferredsMap
      .map {
        case (key, value) =>
          new CountToManyDeferredResolver().resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val toOneFutureResults = toOneDeferredMap
      .map {
        case (key, value) =>
          new ToOneDeferredResolver().resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val oneFutureResult = oneDeferredsMap
      .map {
        case (key, value) =>
          new OneDeferredResolver().resolve(value, ctx.dataResolver)
      }
      .toVector
      .flatten

    val checkScalarFieldPermissionsFutureResults =
      checkScalarFieldPermissionsDeferredsMap
        .map {
          case (key, value) =>
            checkScalarFieldPermissionsDeferredResolver.resolve(value, ctx.dataResolver)
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
      checkScalarFieldPermissionsFutureResults).sortBy(_.order).map(_.future)
  }
}
