package cool.graph.system.database.finder

import cool.graph.shared.models.Project
import cool.graph.system.database.tables._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.QueryBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectQueries(implicit internalDatabase: DatabaseDef, projectResolver: ProjectResolver) {
  import Tables._

  def loadById(id: String): Future[Option[Project]] = {
    // here we explicitly just want to load by id. We do not want the magic fallback to the alias.
    val projectWithIdExists = Projects.filter(p => p.id === id).exists

    internalDatabase.run(projectWithIdExists.result).flatMap {
      case true  => loadByIdOrAlias(id)
      case false => Future.successful(None)
    }
  }

  def loadByIdOrAlias(idOrAlias: String): Future[Option[Project]] = resolveProject(Some(idOrAlias))

  def loadByModelId(modelId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      model <- Models if model.id === modelId
    } yield model.projectId
  }

  def loadByName(clientId: String, name: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      seat    <- Seats if seat.clientId === clientId
      project <- Projects if project.id === seat.projectId && project.name === name
    } yield project.id
  }

  def loadByFieldId(fieldId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      field <- Fields if field.id === fieldId
      model <- Models if field.modelId === model.id
    } yield model.projectId
  }

  def loadByFunctionId(functionId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      function <- Functions if function.id === functionId
    } yield function.projectId
  }

  def loadByEnumId(enumId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      enum <- Enums if enum.id === enumId
    } yield enum.projectId
  }

  def loadByFieldConstraintId(fieldConstraintId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      constraint <- FieldConstraints if constraint.id === fieldConstraintId
      field      <- Fields if field.id === constraint.fieldId
      model      <- Models if field.modelId === model.id
    } yield model.projectId
  }

  def loadByModelPermissionId(modelPermissionId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      permission <- ModelPermissions if permission.id === modelPermissionId
      model      <- Models if model.id === permission.modelId
    } yield model.projectId
  }

  def loadByRelationPermissionId(relationPermissionId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        permission <- RelationPermissions if permission.id === relationPermissionId
        relation   <- Relations if relation.id === permission.relationId
      } yield relation.projectId
    }

  def loadByloadByActionTriggerMutationModelId(actionTriggerId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        mutationTrigger <- ActionTriggerMutationModels if mutationTrigger.id === actionTriggerId
        action          <- Actions if action.id === mutationTrigger.actionId
      } yield action.projectId
    }

  def loadByloadByActionTriggerMutationRelationId(actionTriggerId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        relationTrigger <- ActionTriggerMutationRelations if relationTrigger.id === actionTriggerId
        action          <- Actions if action.id === relationTrigger.actionId
      } yield action.projectId
    }

  def loadByloadByActionactionHandlerWebhookId(actionHandlerId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        webhookAction <- ActionHandlerWebhooks if webhookAction.id === actionHandlerId
        action        <- Actions if webhookAction.actionId === action.id
      } yield action.projectId
    }

  def loadByActionId(actionId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      action <- Actions if action.id === actionId
    } yield action.projectId
  }

  def loadByRelationId(relationId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      relation <- Relations if relation.id === relationId
    } yield relation.projectId
  }

  def loadByRelationFieldMirrorId(relationFieldMirrorId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        relationMirror <- RelationFieldMirrors if relationMirror.id === relationFieldMirrorId
        relation       <- Relations if relation.id === relationMirror.relationId
      } yield relation.projectId
    }

  def loadByIntegrationId(integrationId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      integration <- Integrations if integration.id === integrationId
    } yield integration.projectId
  }

  def loadByAlgoliaSyncQueryId(algoliaSyncQueryId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      algoliaSyncQuery <- AlgoliaSyncQueries if algoliaSyncQuery.id === algoliaSyncQueryId
      model            <- Models if model.id === algoliaSyncQuery.modelId
    } yield model.projectId
  }

  def loadBySeatId(seatId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      seat <- Seats if seat.id === seatId
    } yield seat.projectId
  }

  def loadByPackageDefinitionId(packageDefinitionId: String): Future[Option[Project]] =
    resolveProjectByProjectIdsQuery {
      for {
        definition <- PackageDefinitions if definition.id === packageDefinitionId
      } yield definition.projectId
    }

  def loadByRootTokenId(patId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      rootToken <- RootTokens if rootToken.id === patId
    } yield rootToken.projectId
  }

  def loadByAuthProviderId(authProviderId: String): Future[Option[Project]] = resolveProjectByProjectIdsQuery {
    for {
      integration <- Integrations if integration.id === authProviderId
    } yield integration.projectId
  }

  private def resolveProjectByProjectIdsQuery(projectIdsQuery: QueryBase[Seq[String]]): Future[Option[Project]] = {
    for {
      projectIds <- internalDatabase.run(projectIdsQuery.result)
      project    <- resolveProject(projectIds.headOption)
    } yield project
  }

  private def resolveProject(projectId: Option[String]): Future[Option[Project]] = {
    projectId match {
      case Some(projectId) =>
        projectResolver.resolve(projectId)
      case None =>
        Future.successful(None)
    }
  }
}
