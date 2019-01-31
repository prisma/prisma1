package com.prisma.api.resolver

import com.prisma.api.resolver.DeferredTypes._
import sangria.execution.deferred.Deferred

object DeferredUtils {
  def tagDeferredByOrder[T](deferredValues: Vector[Deferred[T]]): Vector[OrderedDeferred[Deferred[T]]] = {
    deferredValues.zipWithIndex.map {
      case (deferred, order) => OrderedDeferred[Deferred[T]](deferred, order)
    }
  }

  def groupGetNodeDeferreds(deferreds: Vector[OrderedDeferred[GetNodeDeferred]]): Map[_, Vector[OrderedDeferred[GetNodeDeferred]]] = {
    deferreds.groupBy(ordered => (ordered.deferred.where.model, ordered.deferred.where.field))
  }

  def groupGetNodesDeferreds[T <: NodeWithArgsDeferred[Any]](deferreds: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    deferreds.groupBy(ordered => (ordered.deferred.model, ordered.deferred.args))
  }

  def groupScalarListDeferreds(oneDeferred: Vector[OrderedDeferred[ScalarListDeferred]]): Map[_, Vector[OrderedDeferred[ScalarListDeferred]]] = {
    oneDeferred.groupBy(ordered => ordered.deferred.field)
  }

  def groupRelatedDeferred[T <: NodeByParentDeferred[Any]](relatedDeferral: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    relatedDeferral.groupBy(ordered => (ordered.deferred.parentField.relation, ordered.deferred.parentField.relationSide, ordered.deferred.args))
  }
}
