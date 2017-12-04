package cool.graph.deploy.schema

trait DeployApiError extends Exception {
  def message: String
  def errorCode: Int

  override def getMessage: String = message
}

abstract class AbstractDeployApiError(val message: String, val errorCode: Int) extends DeployApiError

case class InvalidProjectId(projectId: String) extends AbstractDeployApiError(s"No service with id '$projectId'", 4000)

case class InvalidServiceName(name: String)
    extends AbstractDeployApiError(s"$name is not a valid name for a service. It may contain up to 150 letters, numbers, underscores and hyphens.", 4001)

case class InvalidServiceStage(stage: String)
    extends AbstractDeployApiError(s"$stage is not a valid name for a service stage. It may contain up to 30 letters, numbers, underscores and hyphens.", 4002)

case class InvalidName(name: String, entityType: String) extends AbstractDeployApiError(InvalidNames.default(name, entityType), 2008)

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."

  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a $entityType."
}
