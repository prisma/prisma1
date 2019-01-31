package com.prisma.api.resolver

import com.prisma.api.connector.DataResolver
import com.prisma.api.resolver.DeferredTypes._
import sangria.execution.deferred.{Deferred, DeferredResolver}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolverImpl[CtxType](dataResolver: DataResolver) extends DeferredResolver[CtxType] {

  val getNodeDeferredResolver          = new GetNodeDeferredResolver(dataResolver)
  val getNodeByParentDeferredResolver  = new GetNodeByParentDeferredResolver(dataResolver)
  val getNodesDeferredResolver         = new GetNodesDeferredResolver(dataResolver)
  val getNodesByParentDeferredResolver = new GetNodesByParentDeferredResolver(dataResolver)
  val countNodesDeferredResolver       = new CountNodesDeferredResolver(dataResolver)
  val scalarListDeferredResolver       = new ScalarListDeferredResolver(dataResolver)

  override def resolve(deferred: Vector[Deferred[Any]], ctx: CtxType, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {

    // group orderedDeferreds by type
    val orderedDeferred = DeferredUtils.tagDeferredByOrder(deferred)

    val getNodeDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodeDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val getNodeByParentDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodeByParentDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val getNodesDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodesDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val getNodesByParentDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: GetNodesByParentDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val countNodesDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: CountNodesDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    val scalarListDeferreds = orderedDeferred.collect {
      case OrderedDeferred(deferred: ScalarListDeferred, order) =>
        OrderedDeferred(deferred, order)
    }

    // for every group, further break them down by their arguments
    val getNodeDeferredMap           = DeferredUtils.groupNodeDeferreds(getNodeDeferreds)
    val getNodeByParentDeferredMap   = DeferredUtils.groupNodeByParentDeferreds[GetNodeByParentDeferred](getNodeByParentDeferreds)
    val getNodesDeferredsMap         = DeferredUtils.groupNodesDeferreds[GetNodesDeferred](getNodesDeferreds)
    val getNodesByParentDeferredsMap = DeferredUtils.groupNodeByParentDeferreds[GetNodesByParentDeferred](getNodesByParentDeferreds)
    val countNodesDeferredsMap       = DeferredUtils.groupNodesDeferreds[CountNodesDeferred](countNodesDeferreds)
    val scalarListDeferredsMap       = DeferredUtils.groupScalarListDeferreds(scalarListDeferreds)

//    val getNodeFutureResults = getNodeDeferreds.map(deferred => oneDeferredResolver.resolve(deferred))

    val getNodeFutureResults = getNodeDeferredMap
      .map {
        case (_, value) => getNodeDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val getNodeByParentFutureResults = getNodeByParentDeferredMap
      .map {
        case (_, value) => getNodeByParentDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val getNodesFutureResults = getNodesDeferredsMap
      .map {
        case (_, value) => getNodesDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val getNodesByParentFutureResults = getNodesByParentDeferredsMap
      .map {
        case (_, value) => getNodesByParentDeferredResolver.resolve(value)
      }
      .toVector
      .flatten

    val countNodesFutureResults = countNodesDeferredsMap
      .map {
        case (_, value) => countNodesDeferredResolver.resolve(value)
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

    (getNodeFutureResults ++
      getNodeByParentFutureResults ++
      getNodesFutureResults ++
      getNodesByParentFutureResults ++
      countNodesFutureResults ++
      scalarListFutureResult ++
      connectionFutureResult).sortBy(_.order).map(_.future)

  }
}
