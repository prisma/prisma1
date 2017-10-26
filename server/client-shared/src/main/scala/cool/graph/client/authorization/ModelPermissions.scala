package cool.graph.client.authorization

import cool.graph.client.mutations.CoolArgs
import cool.graph.shared.models._

object ModelPermissions {
  def checkReadPermissionsForField(
      model: Model,
      field: Field,
      authenticatedRequest: Option[AuthenticatedRequest],
      project: Project
  ): Boolean = {
    checkGlobalStarPermissionFirst(project) {
      checkPermissionsForField(model, field, ModelOperation.Read, authenticatedRequest)
    }
  }

  def checkPermissionsForDelete(
      model: Model,
      authenticatedRequest: Option[AuthenticatedRequest],
      project: Project
  ): Boolean = {
    checkGlobalStarPermissionFirst(project) {
      checkPermissionsForModel(model, ModelOperation.Delete, authenticatedRequest)
    }
  }

  def checkPermissionsForCreate(
      model: Model,
      args: CoolArgs,
      authenticatedRequest: Option[AuthenticatedRequest],
      project: Project
  ): Boolean = {
    checkGlobalStarPermissionFirst(project) {
      val specialAuthProviderRule = project.hasEnabledAuthProvider && model.name == "User"
      specialAuthProviderRule || checkWritePermissions(model, args, authenticatedRequest, ModelOperation.Create, project)
    }
  }

  def checkPermissionsForUpdate(
      model: Model,
      args: CoolArgs,
      authenticatedRequest: Option[AuthenticatedRequest],
      project: Project
  ): Boolean = {
    checkGlobalStarPermissionFirst(project) {
      checkWritePermissions(model, args, authenticatedRequest, ModelOperation.Update, project)
    }
  }

  private def checkGlobalStarPermissionFirst(project: Project)(fallbackCheck: => Boolean): Boolean = {
    project.hasGlobalStarPermission || fallbackCheck
  }

  private def checkWritePermissions(
      model: Model,
      args: CoolArgs,
      authenticatedRequest: Option[AuthenticatedRequest],
      operation: ModelOperation.Value,
      project: Project
  ): Boolean = {
    checkPermissionsForModel(model, operation, authenticatedRequest) &&
    checkPermissionsForScalarFields(model, args, authenticatedRequest, operation, project) &&
    checkPermissionsForRelations(model, args, authenticatedRequest, project)
  }

  private def checkPermissionsForScalarFields(
      model: Model,
      args: CoolArgs,
      authenticatedRequest: Option[AuthenticatedRequest],
      operation: ModelOperation.Value,
      project: Project
  ): Boolean = {
    val checks = for {
      field <- model.scalarFields if field.name != "id"
      if args.hasArgFor(field)
    } yield {
      checkPermissionsForField(model, field, operation, authenticatedRequest)
    }
    checks.forall(identity)
  }

  private def checkPermissionsForRelations(
      model: Model,
      args: CoolArgs,
      authenticatedRequest: Option[AuthenticatedRequest],
      project: Project
  ): Boolean = {
    val subModelChecks = for {
      field    <- model.relationFields
      subArgs  <- args.subArgsList(field).getOrElse(Seq.empty)
      subModel = field.relatedModel(project).get
    } yield {
      checkWritePermissions(subModel, subArgs, authenticatedRequest, ModelOperation.Create, project)
    }
    subModelChecks.forall(identity)
  }

  private def checkPermissionsForField(
      model: Model,
      field: Field,
      operation: ModelOperation.Value,
      authenticatedRequest: Option[AuthenticatedRequest]
  ): Boolean = {
    val permissionsForField = getPermissionsForOperationAndUser(model, operation, authenticatedRequest)
      .filter(p => p.applyToWholeModel || p.fieldIds.contains(field.id))
      .filter(_.isNotCustom)

    if (authenticatedRequest.exists(_.isAdmin)) {
      true
    } else {
      permissionsForField.nonEmpty
    }
  }

  private def checkPermissionsForModel(
      model: Model,
      operation: ModelOperation.Value,
      authenticatedRequest: Option[AuthenticatedRequest]
  ): Boolean = {
    val permissionsForModel = getPermissionsForOperationAndUser(model, operation, authenticatedRequest).filter(_.isNotCustom)

    if (authenticatedRequest.exists(_.isAdmin)) {
      true
    } else {
      permissionsForModel.nonEmpty
    }
  }

  private def getPermissionsForOperationAndUser(
      model: Model,
      operation: ModelOperation.Value,
      authenticatedRequest: Option[AuthenticatedRequest]
  ): List[ModelPermission] = {
    val permissionsForUser = authenticatedRequest.isDefined match {
      case true  => model.permissions
      case false => model.permissions.filter(p => p.userType == UserType.Everyone)
    }

    val permissionsForOperation = permissionsForUser
      .filter(_.isActive)
      .filter(_.operation == operation)

    permissionsForOperation
  }
}
