package cool.graph.deploy.schema

trait SystemApiError extends Exception {
  def message: String
  def errorCode: Int
}

abstract class AbstractSystemApiError(val message: String, val errorCode: Int) extends SystemApiError

case class InvalidProjectId(projectId: String) extends AbstractSystemApiError(s"No service with id '$projectId'", 4000)
