package cool.graph.deploy.schema

trait SystemApiError extends Exception {
  def message: String
  def errorCode: Int
}

abstract class AbstractSystemApiError(val message: String, val errorCode: Int) extends SystemApiError

case class InvalidProjectId(projectId: String) extends AbstractSystemApiError(s"No service with id '$projectId'", 4000)

case class InvalidName(name: String, entityType: String) extends AbstractSystemApiError(InvalidNames.default(name, entityType), 2008)

object InvalidNames {
  def mustStartUppercase(name: String, entityType: String): String =
    s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."
  def default(name: String, entityType: String): String = s"'$name' is not a valid name for a$entityType."
}
