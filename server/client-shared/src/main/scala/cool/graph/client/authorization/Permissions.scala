package cool.graph.client.authorization

import cool.graph.shared.models._

object Permissions {
  def checkNormalPermissionsForField(model: Model,
                                     operation: ModelOperation.Value,
                                     field: Field,
                                     authenticatedRequest: Option[AuthenticatedRequest]): Boolean = {
    val permissionsForField = Permissions
      .permissionsForOperationAndUser(model, operation, authenticatedRequest)
      .filter(p => p.applyToWholeModel || p.fieldIds.contains(field.id))
      .filter(_.isNotCustom)

    if (Permissions.isAdmin(authenticatedRequest)) {
      true
    } else {
      permissionsForField.nonEmpty
    }
  }

  def checkPermissionsForOperationAndUser(model: Model, operation: ModelOperation.Value, authenticatedRequest: Option[AuthenticatedRequest]): Boolean = {
    permissionsForOperationAndUser(model, operation, authenticatedRequest).exists(_.isNotCustom) || isAdmin(authenticatedRequest)
  }

  def permissionsForOperationAndUser(model: Model,
                                     operation: ModelOperation.Value,
                                     authenticatedRequest: Option[AuthenticatedRequest]): List[ModelPermission] = {
    val permissionsForUser = authenticatedRequest.isDefined match {
      case true  => model.permissions
      case false => model.permissions.filter(p => p.userType == UserType.Everyone)
    }

    val permissionsForOperation = permissionsForUser
      .filter(_.isActive)
      .filter(_.operation == operation)

    permissionsForOperation
  }

  def isAdmin(authenticatedRequest: Option[AuthenticatedRequest]): Boolean = authenticatedRequest match {
    case Some(_: AuthenticatedCustomer)  => true
    case Some(_: AuthenticatedRootToken) => true
    case Some(_: AuthenticatedUser)      => false
    case None                            => false
  }
}
