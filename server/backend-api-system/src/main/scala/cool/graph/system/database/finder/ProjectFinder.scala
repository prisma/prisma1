package cool.graph.system.database.finder

import cool.graph.Types.Id
import cool.graph.shared.errors.SystemErrors._
import cool.graph.shared.errors.UserFacingError
import cool.graph.shared.errors.UserInputErrors.InvalidRootTokenId
import cool.graph.shared.models.Project
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

// all load functions take a clientId to enforce permission checks
object ProjectFinder {
  import scala.concurrent.ExecutionContext.Implicits.global

  def loadById(clientId: Id, id: Id)(implicit projectResolver: ProjectResolver): Future[Project] = {
    val projectFuture = projectResolver.resolve(projectIdOrAlias = id)
    checkProject(clientId, InvalidProjectId(id), projectFuture)
  }

  def loadByName(clientId: Id, name: String)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidProjectName(name), ProjectQueries().loadByName(clientId, name))
  }

  def loadByModelId(clientId: Id, modelId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidModelId(modelId), ProjectQueries().loadByModelId(modelId))
  }

  def loadByFieldId(clientId: Id, fieldId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidFieldId(fieldId), ProjectQueries().loadByFieldId(fieldId))
  }

  def loadByEnumId(clientId: Id, enumId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidEnumId(enumId), ProjectQueries().loadByEnumId(enumId))
  }

  def loadByFieldConstraintId(clientId: Id, fieldConstraintId: Id)(implicit internalDatabase: DatabaseDef,
                                                                   projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidFieldConstraintId(fieldConstraintId), ProjectQueries().loadByFieldConstraintId(fieldConstraintId))
  }

  def loadByActionId(clientId: Id, actionId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidActionId(actionId), ProjectQueries().loadByActionId(actionId))
  }

  def loadByFunctionId(clientId: Id, functionId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidFunctionId(functionId), ProjectQueries().loadByFunctionId(functionId))
  }

  def loadByRelationId(clientId: Id, relationId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidRelationId(relationId), ProjectQueries().loadByRelationId(relationId))
  }

  def loadByRelationFieldMirrorId(clientId: Id, relationFieldMirrorId: Id)(implicit internalDatabase: DatabaseDef,
                                                                           projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidRelationFieldMirrorId(relationFieldMirrorId), ProjectQueries().loadByRelationFieldMirrorId(relationFieldMirrorId))
  }

  def loadByActionTriggerMutationModelId(clientId: Id, actionTriggerModelId: Id)(implicit internalDatabase: DatabaseDef,
                                                                                 projectResolver: ProjectResolver): Future[Project] = {
    checkProject(
      clientId,
      InvalidActionTriggerMutationModelId(actionTriggerModelId),
      ProjectQueries().loadByloadByActionTriggerMutationModelId(actionTriggerModelId)
    )
  }

  def loadByActionTriggerMutationRelationId(clientId: Id, actionTriggerRelationId: Id)(implicit internalDatabase: DatabaseDef,
                                                                                       projectResolver: ProjectResolver): Future[Project] = {
    checkProject(
      clientId,
      InvalidActionTriggerMutationModelId(actionTriggerRelationId),
      ProjectQueries().loadByloadByActionTriggerMutationModelId(actionTriggerRelationId)
    )
  }

  def loadByActionHandlerWebhookId(clientId: Id, actionHandlerWebhookId: Id)(implicit internalDatabase: DatabaseDef,
                                                                             projectResolver: ProjectResolver): Future[Project] = {
    checkProject(
      clientId,
      InvalidActionTriggerMutationModelId(actionHandlerWebhookId),
      ProjectQueries().loadByloadByActionactionHandlerWebhookId(actionHandlerWebhookId)
    )
  }

  def loadByModelPermissionId(clientId: Id, modelPermissionId: Id)(implicit internalDatabase: DatabaseDef,
                                                                   projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidModelPermissionId(modelPermissionId), ProjectQueries().loadByModelPermissionId(modelPermissionId))
  }

  def loadByRelationPermissionId(clientId: Id, relationPermissionId: Id)(implicit internalDatabase: DatabaseDef,
                                                                         projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidRelationPermissionId(relationPermissionId), ProjectQueries().loadByRelationPermissionId(relationPermissionId))
  }

  def loadByIntegrationId(clientId: Id, integrationId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidIntegrationId(integrationId), ProjectQueries().loadByIntegrationId(integrationId))
  }

  def loadByAlgoliaSyncQueryId(clientId: Id, algoliaSyncQueryId: Id)(implicit internalDatabase: DatabaseDef,
                                                                     projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidAlgoliaSyncQueryId(algoliaSyncQueryId), ProjectQueries().loadByAlgoliaSyncQueryId(algoliaSyncQueryId))
  }

  def loadBySeatId(clientId: Id, seatId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidSeatId(seatId), ProjectQueries().loadBySeatId(seatId))
  }

  def loadByPackageDefinitionId(clientId: Id, packageDefinitionId: Id)(implicit internalDatabase: DatabaseDef,
                                                                       projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidPackageDefinitionId(packageDefinitionId), ProjectQueries().loadByPackageDefinitionId(packageDefinitionId))
  }

  def loadByRootTokenId(clientId: Id, patId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidRootTokenId(patId), ProjectQueries().loadByRootTokenId(patId))
  }

  def loadByAuthProviderId(clientId: Id, authProviderId: Id)(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver): Future[Project] = {
    checkProject(clientId, InvalidAuthProviderId(authProviderId), ProjectQueries().loadByAuthProviderId(authProviderId))
  }

  private def checkProject(clientId: Id, error: UserFacingError, projectFuture: Future[Option[Project]]): Future[Project] = {
    projectFuture.map {
      case Some(project) => if (project.seats.exists(_.clientId.contains(clientId))) project else throw error
      case None          => throw error
    }
  }
}
