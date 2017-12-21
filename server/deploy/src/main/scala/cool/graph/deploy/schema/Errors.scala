package cool.graph.deploy.schema

import cool.graph.gc_values.InvalidValueForScalarType
import cool.graph.shared.models.ProjectId

trait DeployApiError extends Exception {
  def message: String
  def errorCode: Int

  override def getMessage: String = message
}

abstract class AbstractDeployApiError(val message: String, val errorCode: Int) extends DeployApiError

case class InvalidProjectId(projectId: String)
    extends AbstractDeployApiError({
      val nameAndStage = ProjectId.fromEncodedString(projectId)
      s"No service with name '${nameAndStage.name}' and stage '${nameAndStage.stage}' found"
    }, 4000)

case class InvalidServiceName(name: String) extends AbstractDeployApiError(InvalidNames.forService(name, "service name"), 4001)

case class InvalidServiceStage(stage: String) extends AbstractDeployApiError(InvalidNames.forService(stage, "service stage"), 4002)

case class InvalidName(name: String, entityType: String) extends AbstractDeployApiError(InvalidNames.default(name, entityType), 2008)

case class InvalidDeployment(deployErrorMessage: String) extends AbstractDeployApiError(deployErrorMessage, 4003)

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."

  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a $entityType."

  def forService(value: String, tpe: String) = {
    s"$value is not a valid name for a $tpe. It must start with a letter and may contain up to 30 letters, numbers, underscores and hyphens."
  }
}
