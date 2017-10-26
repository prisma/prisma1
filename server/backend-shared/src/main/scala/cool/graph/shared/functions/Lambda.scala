package cool.graph.shared.functions

import cool.graph.shared.models.Project

import scala.concurrent.Future

trait FunctionEnvironment {
  def getTemporaryUploadUrl(project: Project): Future[String]
  def deploy(project: Project, externalFile: ExternalFile, name: String): Future[DeployResponse]
  def invoke(project: Project, name: String, event: String): Future[InvokeResponse]
}

sealed trait DeployResponse
case class DeploySuccess()                     extends DeployResponse
case class DeployFailure(exception: Throwable) extends DeployResponse

sealed trait InvokeResponse
case class InvokeSuccess(returnValue: String)  extends InvokeResponse
case class InvokeFailure(exception: Throwable) extends InvokeResponse

case class ExternalFile(url: String, lambdaHandler: String, devHandler: String, hash: Option[String])