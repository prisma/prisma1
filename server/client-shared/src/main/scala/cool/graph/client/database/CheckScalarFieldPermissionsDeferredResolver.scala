package cool.graph.client.database

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.DataItem
import cool.graph.client.authorization.{ModelPermissions, PermissionQueryArg, PermissionValidator}
import cool.graph.client.database.DeferredTypes._
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models._
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckScalarFieldPermissionsDeferredResolver(skipPermissionCheck: Boolean, project: Project)(implicit inj: Injector) extends Injectable {

  implicit val system = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer =
    inject[ActorMaterializer](identified by "actorMaterializer")
  val permissionValidator = new PermissionValidator(project)

  def resolve(orderedDefereds: Vector[OrderedDeferred[CheckPermissionDeferred]], ctx: DataResolver): Vector[OrderedDeferredFutureResult[Any]] = {
    val deferreds = orderedDefereds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfPermissionDeferredsAndThrow(deferreds)

    if (skipPermissionCheck) {
      return orderedDefereds.map(x => OrderedDeferredFutureResult[Any](Future.successful(x.deferred.value), x.order))
    }

    val headDeferred = deferreds.head

    val model                  = headDeferred.model
    val authenticatedRequest   = headDeferred.authenticatedRequest
    val fieldsToCover          = orderedDefereds.map(_.deferred.field).distinct
    val allPermissions         = headDeferred.model.permissions.filter(_.isActive).filter(_.operation == ModelOperation.Read)
    val wholeModelPermissions  = allPermissions.filter(_.applyToWholeModel)
    val singleFieldPermissions = allPermissions.filter(!_.applyToWholeModel)

    def checkSimplePermissions(remainingFields: List[Field]): Future[List[Field]] = {
      Future.successful(remainingFields.filter(field => !ModelPermissions.checkReadPermissionsForField(model, field, authenticatedRequest, project)))
    }

    def checkWholeModelPermissions(remainingFields: List[Field]): Future[List[Field]] = {
      if (remainingFields.isEmpty) {
        Future.successful(List())
      }
      checkQueryPermissions(authenticatedRequest, wholeModelPermissions, headDeferred.nodeId, model, headDeferred.node, headDeferred.alwaysQueryMasterDatabase)
        .map(wasSuccess => {
          if (wasSuccess) {
            List()
          } else {
            remainingFields
          }
        })
    }

    def checkIndividualFieldPermissions(remainingFields: List[Field], remainingPermissions: List[ModelPermission]): Future[List[Field]] = {
      if (remainingPermissions.isEmpty || remainingFields.isEmpty) {
        Future.successful(remainingFields)

      } else {

        val (current, rest) = getMostLikelyPermission(remainingFields, remainingPermissions)
        checkQueryPermissions(authenticatedRequest, List(current), headDeferred.nodeId, model, headDeferred.node, headDeferred.alwaysQueryMasterDatabase)
          .flatMap(wasSuccess => {
            if (wasSuccess) {
              checkIndividualFieldPermissions(remainingFields.filter(x => !current.fieldIds.contains(x.id)), rest)
            } else {
              checkIndividualFieldPermissions(remainingFields, rest)
            }
          })
      }
    }

    def getMostLikelyPermission(remainingFields: List[Field], remainingPermissions: List[ModelPermission]): (ModelPermission, List[ModelPermission]) = {
      val current: ModelPermission =
        remainingPermissions.maxBy(p => remainingFields.map(_.id).intersect(p.fieldIds).length)
      val rest = remainingPermissions.filter(_ != current)

      (current, rest)
    }

    val disallowedFieldIds: Future[List[Field]] = for {
      remainingAfterSimplePermissions      <- checkSimplePermissions(fieldsToCover.toList)
      remainingAfterAllModelPermissions    <- checkWholeModelPermissions(remainingAfterSimplePermissions)
      remainingAfterSingleFieldPermissions <- checkIndividualFieldPermissions(remainingAfterAllModelPermissions, singleFieldPermissions)
    } yield {
      remainingAfterSingleFieldPermissions
    }

    def deferredToResultOrError(deferred: CheckPermissionDeferred) = {
      disallowedFieldIds.map(x => {
        if (x.map(_.id).contains(deferred.field.id)) {
          throw UserAPIErrors.InsufficientPermissions("Insufficient Permissions")
        } else {
          deferred.value
        }
      })
    }

    // assign the DataItem that was requested by each deferred
    orderedDefereds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[Any](deferredToResultOrError(deferred), order)
    }
  }

  def checkQueryPermissions(authenticatedRequest: Option[AuthenticatedRequest],
                            permissions: List[ModelPermission],
                            nodeId: String,
                            model: Model,
                            node: DataItem,
                            alwaysQueryMasterDatabase: Boolean): Future[Boolean] = {
    val args = model.scalarFields.map(field => PermissionQueryArg(s"$$node_${field.name}", node.getOption(field.name).getOrElse(""), field.typeIdentifier))

    permissionValidator.checkModelQueryPermissions(project, permissions, authenticatedRequest, nodeId, args, alwaysQueryMasterDatabase)
  }

}
