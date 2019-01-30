package com.prisma.api.resolver

import com.prisma.api.resolver.DeferredTypes._
import sangria.execution.deferred.Deferred

object DeferredUtils {
  def tagDeferredByOrder[T](deferredValues: Vector[Deferred[T]]): Vector[OrderedDeferred[Deferred[T]]] = {
    deferredValues.zipWithIndex.map {
      case (deferred, order) => OrderedDeferred[Deferred[T]](deferred, order)
    }
  }

  def groupModelWhereDeferred(oneDeferred: Vector[OrderedDeferred[OneDeferred]]): Map[_, Vector[OrderedDeferred[OneDeferred]]] = {
    oneDeferred.groupBy(ordered => (ordered.deferred.where.model, ordered.deferred.where.field))
  }

  def groupModelArgsDeferred[T <: ModelDeferred[Any]](modelDeferred: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    modelDeferred.groupBy(ordered => (ordered.deferred.model, ordered.deferred.args))
  }

  def groupModelExistsDeferred[T <: ModelDeferred[Any]](modelExistsDeferred: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    modelExistsDeferred.groupBy(ordered => (ordered.deferred.model, ordered.deferred.args))
  }

  def groupOneDeferred[T <: OneDeferred](oneDeferred: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    oneDeferred.groupBy(ordered => ordered.deferred.model)
  }

  def groupScalarListDeferreds[T <: ScalarListDeferred](oneDeferred: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    oneDeferred.groupBy(ordered => ordered.deferred.field)
  }

  def groupRelatedDeferred[T <: RelationDeferred[Any]](relatedDeferral: Vector[OrderedDeferred[T]]): Map[_, Vector[OrderedDeferred[T]]] = {
    relatedDeferral.groupBy(ordered => (ordered.deferred.relationField.relation, ordered.deferred.relationField.relationSide, ordered.deferred.args))
  }

  def checkSimilarityOfModelDeferredsAndThrow(deferreds: Vector[ModelDeferred[Any]]) = {
    val headDeferred = deferreds.head
    val args         = headDeferred.args

    val countSimilarDeferreds = deferreds.count { deferred =>
      deferred.model.name == deferred.model.name &&
      deferred.args == args
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not belong to different relations and should not have different arguments.")
    }
  }

  def checkSimilarityOfRelatedDeferredsAndThrow(deferreds: Vector[RelationDeferred[Any]]) = {
    val headDeferred = deferreds.head
    val relatedField = headDeferred.relationField
    val args         = headDeferred.args

    val countSimilarDeferreds = deferreds.count { d =>
      val myRelatedField = d.relationField
      myRelatedField.relation == relatedField.relation &&
      myRelatedField.typeIdentifier == relatedField.typeIdentifier &&
      myRelatedField.relationSide == relatedField.relationSide &&
      d.args == args
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not belong to different relations and should not have different arguments.")
    }
  }

  def checkSimilarityOfOneDeferredsAndThrow(deferreds: Vector[OneDeferred]) = {
    val headDeferred = deferreds.head

    val countSimilarDeferreds = deferreds.count { d =>
      d.where.field == headDeferred.where.field &&
      d.model == headDeferred.model
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not have different key or model.")
    }
  }

  def checkSimilarityOfScalarListDeferredsAndThrow(deferreds: Vector[ScalarListDeferred]) = {
    val headDeferred = deferreds.head

    val countSimilarDeferreds = deferreds.count { d =>
      d.field.name == headDeferred.field.name
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not have different field or model.")
    }
  }
}
