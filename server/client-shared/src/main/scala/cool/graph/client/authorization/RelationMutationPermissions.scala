package cool.graph.client.authorization

import cool.graph.client.mutactions._
import cool.graph.shared.models._
import cool.graph.Mutaction
import cool.graph.shared.errors.UserAPIErrors
import scaldi.Injector

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RelationMutationPermissions {

  case class PermissionInput(
      relation: Relation,
      project: Project,
      aId: String,
      bId: String,
      authenticatedRequest: Option[AuthenticatedRequest]
  )

  def checkAllPermissions(
      project: Project,
      mutactions: List[Mutaction],
      authenticatedRequest: Option[AuthenticatedRequest]
  )(implicit inj: Injector): Future[Unit] = {
    if (authenticatedRequest.exists(_.isAdmin) || project.hasGlobalStarPermission) {
      Future.successful(())
    } else {
      val connectPermissions = mutactions collect {
        case m: AddDataItemToManyRelation =>
          PermissionInput(m.relation, m.project, m.aValue, m.bValue, authenticatedRequest)
      }

      val disconnectPermissions = mutactions collect {
        // Remove From Relation and Unset Relation
        case m: RemoveDataItemFromRelationByToAndFromField =>
          PermissionInput(m.project.getRelationById_!(m.relationId), m.project, m.aId, m.bId, authenticatedRequest)

//    There are four more mutactions that are used to disconnect relations, these are used when the disconnect is a side effect.
//    We need to decide how to handle side effect disconnects. The mutactions all have different information available to them,
//    so we would need to document which information permission queries could rely on for these. Especially the ones in the nested
//    case are often called preventively, and the item on which disconnect is checked does not necessarily exist.

//        // Set Relation
//        case m: RemoveDataItemFromRelationById =>
//          PermissionInput(m.project.getRelationById_!(m.relationId), m.project, "", "", authenticatedRequest)
//        // Add To Relation
//        case m: RemoveDataItemFromRelationByField =>
//          PermissionInput(m.field.relation.get, project, "", "", authenticatedRequest)
//        // Nasty Nested create stuff -.-, also deletes, updates
//        case m: RemoveDataItemFromManyRelationByFromId =>
//          PermissionInput(m.fromField.relation.get, project, "", "", authenticatedRequest)
//        case m: RemoveDataItemFromManyRelationByToId =>
//          PermissionInput(m.fromField.relation.get, project, "", "", authenticatedRequest)
      }

      val verifyConnectPermissions = connectPermissions.map(input => {
        if (checkNormalConnectOrDisconnectPermissions(input.relation, input.authenticatedRequest, checkConnect = true, checkDisconnect = false)) {
          Future.successful(())
        } else {
          checkQueryPermissions(project, input.relation, authenticatedRequest, input.aId, input.bId, checkConnect = true, checkDisconnect = false)
            .map(isValid => if (!isValid) throw UserAPIErrors.InsufficientPermissions("No CONNECT permissions"))
        }
      })

      val verifyDisconnectPermissions = disconnectPermissions.map(input => {
        if (checkNormalConnectOrDisconnectPermissions(input.relation, input.authenticatedRequest, checkConnect = false, checkDisconnect = true)) {
          Future.successful(())
        } else {
          checkQueryPermissions(project, input.relation, authenticatedRequest, input.aId, input.bId, checkConnect = false, checkDisconnect = true)
            .map(isValid => if (!isValid) throw UserAPIErrors.InsufficientPermissions("No DISCONNECT permissions"))
        }
      })

      Future.sequence(verifyConnectPermissions ++ verifyDisconnectPermissions).map(_ => ())
    }
  }

  private def checkNormalConnectOrDisconnectPermissions(
      relation: Relation,
      authenticatedRequest: Option[AuthenticatedRequest],
      checkConnect: Boolean,
      checkDisconnect: Boolean
  ): Boolean = {

    val permissionsForUser = authenticatedRequest.isDefined match {
      case true  => relation.permissions
      case false => relation.permissions.filter(p => p.userType == UserType.Everyone)
    }

    permissionsForUser
      .filter(_.isActive)
      .filter(_.connect || !checkConnect)
      .filter(_.disconnect || !checkDisconnect)
      .exists(_.isNotCustom)
  }

  private def checkQueryPermissions(
      project: Project,
      relation: Relation,
      authenticatedRequest: Option[AuthenticatedRequest],
      aId: String,
      bId: String,
      checkConnect: Boolean,
      checkDisconnect: Boolean
  )(implicit inj: Injector): Future[Boolean] = {

    val filteredPermissions = relation.permissions
      .filter(_.isActive)
      .filter(_.connect || !checkConnect)
      .filter(_.disconnect || !checkDisconnect)
      .filter(_.rule == CustomRule.Graph)
      .filter(_.userType == UserType.Everyone || authenticatedRequest.isDefined)

    val arguments = Map(
      "$user_id"                         -> (authenticatedRequest.map(_.id).getOrElse(""), "ID"),
      s"$$${relation.aName(project)}_id" -> (bId, "ID"),
      s"$$${relation.bName(project)}_id" -> (aId, "ID")
    )

    val permissionValidator = new PermissionValidator(project)
    permissionValidator.checkRelationQueryPermissions(project, filteredPermissions, authenticatedRequest, arguments, alwaysQueryMasterDatabase = true)
  }
}
