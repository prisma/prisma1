package com.prisma.deploy.schema

import com.prisma.shared.models.ProjectId

trait DeployApiError extends Exception {
  def message: String
  val code: Int

  override def getMessage: String = message
}

abstract class AbstractDeployApiError(val message: String, val code: Int) extends DeployApiError

// 20xx
//case class InvalidName(name: String, entityType: String) extends AbstractDeployApiError(InvalidNames.default(name, entityType), 2008)

// 30xx
case class AuthFailure(reason: String) extends AbstractDeployApiError(reason, 3015)

object TokenExpired extends AbstractDeployApiError(s"Authentication token is expired", 3016)

case class InvalidQuery(reason: String) extends AbstractDeployApiError(reason, 3017)

case class UpdatedRelationAmbiguous(reason: String) extends AbstractDeployApiError(reason, 3018)

// 40xx
case class InvalidProjectId(projectId: ProjectId)
    extends AbstractDeployApiError({
      s"No service with name '${projectId.name}' and stage '${projectId.stage}' found"
    }, 4000)

case class InvalidServiceName(name: String) extends AbstractDeployApiError(InvalidNames.forService(name, "service name"), 4001)

case class InvalidServiceStage(stage: String) extends AbstractDeployApiError(InvalidNames.forService(stage, "service stage"), 4002)

//case class InvalidDeployment(deployErrorMessage: String) extends AbstractDeployApiError(deployErrorMessage, 4003)

case class RelationNameNeeded(relationName: String, firstA: String, firstB: String, secondA: String, secondB: String)
    extends AbstractDeployApiError(
      "There was an error during the autogeneration of relation names.\n" +
        s"Prisma generated the name $relationName twice, once for:\n" +
        s"A relation between $firstA and $firstB\n" +
        s"A relation between $secondA and $secondB\n" +
        s"Please name at least one of the relations.",
      4004
    )
case class ProjectAlreadyExists(name: String, stage: String)
    extends AbstractDeployApiError(s"Service with name '$name' and stage '$stage' already exists", 4005)

case class ReservedServiceName(name: String) extends AbstractDeployApiError(s"Service name $name is reserved. Please choose a different name.", 4006)
case class ReservedStageName(stage: String)  extends AbstractDeployApiError(s"Stage name $stage is reserved. Please choose a different stage name.", 4007)

object DeploymentInProgress
    extends AbstractDeployApiError(
      "You can not deploy to a service stage while there is a deployment in progress or a pending deployment scheduled already. Please try again after the deployment finished.",
      4008
    )

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."

  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a $entityType."

  def forService(value: String, tpe: String) = {
    s"$value is not a valid name for a $tpe. It must start with a letter and may contain up to 30 letters, numbers, underscores and hyphens."
  }
  def forRelation(value: String, tpe: String) = {
    s"The provided name: $value is not a valid name for a $tpe. It can only have up to 54 characters and must have the shape [A-Z][a-zA-Z0-9]*"
  }
}
