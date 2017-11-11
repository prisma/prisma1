package cool.graph.client.database

import cool.graph.Types.Id
import cool.graph.client.database.DeferredTypes._
import cool.graph.shared.models.{AuthenticatedRequest, Model}
import sangria.execution.deferred.Deferred

object DeferredUtils {
  def tagDeferredByOrder[T](deferredValues: Vector[Deferred[T]]): Vector[OrderedDeferred[Deferred[T]]] = {
    deferredValues.zipWithIndex.map {
      case (deferred, order) => OrderedDeferred[Deferred[T]](deferred, order)
    }
  }

  def groupModelDeferred[T <: ModelDeferred[Any]](modelDeferred: Vector[OrderedDeferred[T]]): Map[(Model, Option[QueryArguments]), Vector[OrderedDeferred[T]]] = {
    modelDeferred.groupBy(ordered => (ordered.deferred.model, ordered.deferred.args))
  }

  def groupModelExistsDeferred[T <: ModelDeferred[Any]](
      modelExistsDeferred: Vector[OrderedDeferred[T]]): Map[(Model, Option[QueryArguments]), Vector[OrderedDeferred[T]]] = {
    modelExistsDeferred.groupBy(ordered => (ordered.deferred.model, ordered.deferred.args))
  }

  def groupOneDeferred[T <: OneDeferred](oneDeferred: Vector[OrderedDeferred[T]]): Map[Model, Vector[OrderedDeferred[T]]] = {
    oneDeferred.groupBy(ordered => ordered.deferred.model)
  }

  def groupRelatedDeferred[T <: RelationDeferred[Any]](
      relatedDeferral: Vector[OrderedDeferred[T]]): Map[(Id, String, Option[QueryArguments]), Vector[OrderedDeferred[T]]] = {
    relatedDeferral.groupBy(ordered =>
      (ordered.deferred.relationField.relation.get.id, ordered.deferred.relationField.relationSide.get.toString, ordered.deferred.args))
  }

  case class PermissionDeferredKey(model: Model, nodeId: String, authenticatedRequest: Option[AuthenticatedRequest])
  def groupPermissionDeferred(
      permissionDeferreds: Vector[OrderedDeferred[CheckPermissionDeferred]]): Map[PermissionDeferredKey, Vector[OrderedDeferred[CheckPermissionDeferred]]] = {
    permissionDeferreds.groupBy(
      ordered => PermissionDeferredKey(ordered.deferred.model, ordered.deferred.nodeId, ordered.deferred.authenticatedRequest)
    )
  }

  def checkSimilarityOfModelDeferredsAndThrow(deferreds: Vector[ModelDeferred[Any]]) = {
    val headDeferred = deferreds.head
    val model        = headDeferred.model
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
      d.key == headDeferred.key &&
      d.model == headDeferred.model
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not have different key or model.")
    }
  }

  def checkSimilarityOfPermissionDeferredsAndThrow(deferreds: Vector[CheckPermissionDeferred]) = {
    val headDeferred = deferreds.head

    val countSimilarDeferreds = deferreds.count { d =>
      headDeferred.nodeId == d.nodeId &&
      headDeferred.model == headDeferred.model &&
      headDeferred.authenticatedRequest == headDeferred.authenticatedRequest
    }

    if (countSimilarDeferreds != deferreds.length) {
      throw new Error("Passed deferreds should not have dirrefent nodeIds, models or userIds.")
    }
  }
}
