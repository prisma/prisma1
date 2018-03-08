package com.prisma.deploy.schema

import com.prisma.sangria.utils.ErrorWithCode
import com.prisma.shared.models.ProjectId

trait DeployApiError extends Exception with ErrorWithCode {
  def message: String
  val code: Int

  override def getMessage: String = message
}

abstract class AbstractDeployApiError(val message: String, val code: Int) extends DeployApiError

case class InvalidName(name: String, entityType: String) extends AbstractDeployApiError(InvalidNames.default(name, entityType), 2008)

case class InvalidProjectId(projectId: String)
    extends AbstractDeployApiError({
      val nameAndStage = ProjectId.fromEncodedString(projectId)
      s"No service with name '${nameAndStage.name}' and stage '${nameAndStage.stage}' found"
    }, 4000)

case class InvalidServiceName(name: String) extends AbstractDeployApiError(InvalidNames.forService(name, "service name"), 4001)

case class InvalidServiceStage(stage: String) extends AbstractDeployApiError(InvalidNames.forService(stage, "service stage"), 4002)

case class InvalidDeployment(deployErrorMessage: String) extends AbstractDeployApiError(deployErrorMessage, 4003)

case class InvalidRelationName(relationName: String) extends AbstractDeployApiError(InvalidNames.forService(relationName, "relation"), 4004)

case class InvalidToken(reason: String) extends AbstractDeployApiError(s"Authentication token is invalid: $reason", 3015)

object TokenExpired extends AbstractDeployApiError(s"Authentication token is expired", 3016)

case class InvalidQuery(reason: String) extends AbstractDeployApiError(reason, 3017)

case class UpdatedRelationAmbigous(reason: String) extends AbstractDeployApiError(reason, 3018)

object DeploymentInProgress
    extends AbstractDeployApiError(
      "You can not deploy to a service stage while there is a deployment in progress or a pending deployment scheduled already. Please try again after the deployment finished.",
      4004
    )

case class ProjectAlreadyExists(name: String, stage: String)
    extends AbstractDeployApiError(s"Service with name '$name' and stage '$stage' already exists", 4005)

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."

  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a $entityType."

  def forService(value: String, tpe: String) = {
    s"$value is not a valid name for a $tpe. It must start with a letter and may contain up to 30 letters, numbers, underscores and hyphens."
  }
  def forRelation(value: String, tpe: String) = {
    s"The provided name: $value is not valid for a $tpe. It can only have up to 54 characters and must have the shape [A-Z][a-zA-Z0-9]*"
  }
}
