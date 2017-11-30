package cool.graph.deploy.schema

trait DeployApiError extends Exception {
  def message: String
  def errorCode: Int

  override def getMessage: String = message
}

abstract class AbstractDeployApiError(val message: String, val errorCode: Int) extends DeployApiError

case class InvalidProjectId(projectId: String) extends AbstractDeployApiError(s"No service with id '$projectId'", 4000)

case class InvalidName(name: String, entityType: String) extends AbstractDeployApiError(InvalidNames.default(name, entityType), 2008)

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."
  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a$entityType."
}
