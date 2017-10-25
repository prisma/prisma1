package cool.graph.shared.functions.dev

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cool.graph.cuid.Cuid
import cool.graph.shared.functions._
import cool.graph.shared.models.Project
import cool.graph.utils.future.FutureUtils._
import play.api.libs.json.{JsError, JsSuccess, Json}
import spray.json.{JsArray, JsObject, JsString}
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DevFunctionEnvironment()(implicit system: ActorSystem, materializer: ActorMaterializer) extends FunctionEnvironment {
  import Conversions._
  import system.dispatcher

  private val akkaHttp = Http()(system)

  val functionEndpointInternal: String =
    sys.env.getOrElse("FUNCTION_ENDPOINT_INTERNAL", sys.error("FUNCTION_ENDPOINT_INTERNAL env var required for dev function deployment.")).stripSuffix("/")

  val functionEndpointExternal: String =
    sys.env.getOrElse("FUNCTION_ENDPOINT_EXTERNAL", sys.error("FUNCTION_ENDPOINT_EXTERNAL env var required for dev function deployment.")).stripSuffix("/")

  override def getTemporaryUploadUrl(project: Project): Future[String] = {
    val deployId = Cuid.createCuid()
    Future.successful(s"$functionEndpointExternal/functions/files/${project.id}/$deployId")
  }

  override def deploy(project: Project, externalFile: ExternalFile, name: String): Future[DeployResponse] = {
    val body = Json.toJson(DeploymentInput(externalFile.url, externalFile.devHandler, name)).toString()

    val akkaRequest = HttpRequest(
      uri = s"$functionEndpointInternal/functions/deploy/${project.id}",
      method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, body.toString)
    )

    akkaHttp.singleRequest(akkaRequest).flatMap(Unmarshal(_).to[String]).toFutureTry.flatMap {
      case Success(responseBody) =>
        Json.parse(responseBody).validate[StatusResponse] match {
          case JsSuccess(status, _) =>
            if (status.success) {
              Future.successful(DeploySuccess())
            } else {
              Future.successful(DeployFailure(new Exception(status.error.getOrElse(""))))
            }

          case JsError(e) =>
            Future.successful(DeployFailure(new Exception(e.toString)))
        }

      case Failure(e) =>
        Future.successful(DeployFailure(e))
    }
  }

  override def invoke(project: Project, name: String, event: String): Future[InvokeResponse] = {
    val body = Json.toJson(FunctionInvocation(name, event)).toString()

    val akkaRequest = HttpRequest(
      uri = s"$functionEndpointInternal/functions/invoke/${project.id}",
      method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, body.toString)
    )

    akkaHttp.singleRequest(akkaRequest).flatMap(Unmarshal(_).to[String]).toFutureTry.flatMap {
      case Success(responseBody) =>
        Json.parse(responseBody).validate[FunctionInvocationResult] match {
          case JsSuccess(result, _) =>
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
              Future.successful(InvokeSuccess(output))
            } else {
              Future.successful(InvokeFailure(new Exception(output)))
            }

          case JsError(e) =>
            Future.successful(InvokeFailure(new Exception(e.toString)))
        }

      case Failure(e) =>
        Future.successful(InvokeFailure(e))
    }
  }

  private def convertResponse(akkaResponse: HttpResponse): Future[String] = Unmarshal(akkaResponse).to[String]
}
