package cool.graph.api.schema

trait ApiError extends Exception {
  def message: String
  def errorCode: Int
}

abstract class AbstractApiError(val message: String, val errorCode: Int) extends ApiError

case class InvalidProjectId(projectId: String) extends AbstractApiError(s"No service with id '$projectId'", 4000)
