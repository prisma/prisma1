package cool.graph.shared.functions.dev

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.SimpleHttpClient
import cool.graph.cuid.Cuid
import cool.graph.shared.functions._
import cool.graph.shared.models.Project
import spray.json.{JsArray, JsObject, JsString, _}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DevFunctionEnvironment()(implicit system: ActorSystem, materializer: ActorMaterializer) extends FunctionEnvironment {
  import Conversions._
  import system.dispatcher

  private val httpClient = SimpleHttpClient()

  val functionEndpointInternal: String =
    sys.env.getOrElse("FUNCTION_ENDPOINT_INTERNAL", sys.error("FUNCTION_ENDPOINT_INTERNAL env var required for dev function deployment.")).stripSuffix("/")

  val functionEndpointExternal: String =
    sys.env.getOrElse("FUNCTION_ENDPOINT_EXTERNAL", sys.error("FUNCTION_ENDPOINT_EXTERNAL env var required for dev function deployment.")).stripSuffix("/")

  override def getTemporaryUploadUrl(project: Project): Future[String] = {
    val deployId = Cuid.createCuid()
    Future.successful(s"$functionEndpointExternal/functions/files/${project.id}/$deployId")
  }

  override def deploy(project: Project, externalFile: ExternalFile, name: String): Future[DeployResponse] = {
    httpClient
      .postJson(s"$functionEndpointInternal/functions/deploy/${project.id}", DeploymentInput(externalFile.url, externalFile.devHandler, name))
      .map { response =>
        response.bodyAs[StatusResponse] match {
          case Success(status) =>
            if (status.success) {
              DeploySuccess()
            } else {
              DeployFailure(new Exception(status.error.getOrElse("")))
            }

          case Failure(e) => DeployFailure(e)
        }
      }
      .recover {
        case e: Throwable => DeployFailure(e)
      }
  }

  override def invoke(project: Project, name: String, event: String): Future[InvokeResponse] = {
    httpClient
      .postJson(s"$functionEndpointInternal/functions/invoke/${project.id}", FunctionInvocation(name, event))
      .map { response =>
        response.bodyAs[FunctionInvocationResult] match {
          case Success(result) =>
            val returnValue = Try { result.value.map(_.toString).getOrElse("").parseJson } match {
              case Success(parsedJson) => parsedJson
              case Failure(_)          => JsObject("error" -> JsString("Function did not return a valid response. Check your function code / logs."))
            }

            val output = JsObject(
              "logs" -> JsArray(
                JsObject("stdout" -> JsString(result.stdout.getOrElse(""))),
                JsObject("stderr" -> JsString(result.stderr.getOrElse(""))),
                JsObject("error"  -> JsString(result.error.getOrElse("")))
              ),
              "response" -> returnValue
            ).compactPrint

            if (result.success) {
              InvokeSuccess(output)
            } else {
              InvokeFailure(new Exception(output))
            }

          case Failure(e) => InvokeFailure(e)
        }
      }
      .recover {
        case e: Throwable => InvokeFailure(e)
      }
  }
}
