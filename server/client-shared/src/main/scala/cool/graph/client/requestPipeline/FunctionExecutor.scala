package cool.graph.client.requestPipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, StreamTcpException}
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.client.authorization.ClientAuthImpl
import cool.graph.cuid.Cuid
import cool.graph.messagebus.{Conversions, QueuePublisher}
import cool.graph.shared.errors.RequestPipelineErrors._
import cool.graph.shared.errors.UserInputErrors.ResolverPayloadIsRequired
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, InvokeFailure, InvokeSuccess}
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.util.collection.ToImmutable._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalactic.{Bad, Good, Or}
import scaldi.{Injectable, Injector}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

sealed trait FunctionResult
case class FunctionSuccess(values: FunctionDataValue, result: FunctionExecutionResult) extends FunctionResult

case class FunctionDataValue(isNull: Boolean, values: Vector[JsObject])

sealed trait FunctionError                                                 extends FunctionResult
case class FunctionReturnedBadStatus(statusCode: Int, rawResponse: String) extends FunctionError
case class FunctionReturnedBadBody(badBody: String, parseError: String)    extends FunctionError
case class FunctionWebhookURLNotValid(url: String)                         extends FunctionError

sealed trait FunctionReturnedError                                                     extends FunctionError
case class FunctionReturnedStringError(error: String, result: FunctionExecutionResult) extends FunctionReturnedError
case class FunctionReturnedJsonError(json: JsObject, result: FunctionExecutionResult)  extends FunctionReturnedError

case class FunctionExecutionResult(logs: Vector[String], returnValue: Map[String, Any])

class FunctionExecutor(implicit val inj: Injector) extends Injectable {
  implicit val actorSystem: ActorSystem        = inject[_root_.akka.actor.ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[_root_.akka.stream.ActorMaterializer](identified by "actorMaterializer")

  val functionEnvironment: FunctionEnvironment = inject[FunctionEnvironment]
  val logsPublisher                            = inject[QueuePublisher[String]](identified by "logsPublisher")

  def sync(project: Project, function: models.Function, event: String): Future[FunctionSuccess Or FunctionError] = {
    function.delivery match {

      // Lambda and Dev function environment

      case delivery: models.ManagedFunction => {
        functionEnvironment.invoke(project, function.name, event) flatMap {
          case InvokeSuccess(response)  => handleSuccessfulResponse(project, response, function, acceptEmptyResponse = false)
          case InvokeFailure(exception) => Future.successful(Bad(FunctionReturnedBadStatus(0, exception.getMessage)))
        }
      }

      // Auth0Extend and Webhooks

      case delivery: models.HttpFunction =>
        val headers = delivery.headers.map { case (name, value) => RawHeader(name, value) }.toImmutable

        val httpExt = Http(actorSystem)
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = function.delivery.asInstanceOf[HttpFunction].url,
          headers = headers,
          entity = HttpEntity(ContentTypes.`application/json`, event)
        )

        val response: Future[HttpResponse] = httpExt.singleRequest(request)

        response.flatMap { serverlessResult =>
          val statusCode = serverlessResult.status.intValue
          if (statusCode >= 200 && statusCode < 300) {
            handleSuccessfulResponse(project, serverlessResult, function, acceptEmptyResponse = statusCode == 204)
          } else {
            Unmarshal(serverlessResult)
              .to[String]
              .map(bodyString => Bad(FunctionReturnedBadStatus(statusCode, bodyString)))
          }
        } recover {
          // https://[INVALID].algolia.net/1/keys/[VALID] times out, so we simply report a timeout as a wrong appId
          case _: StreamTcpException => Bad(FunctionWebhookURLNotValid(request.uri.toString()))
        }
      case _ => sys.error("only knows how to execute HttpFunctions")
    }
  }

  def syncWithLogging(function: models.Function, event: String, project: Project, requestId: String): Future[FunctionSuccess Or FunctionError] = {
    val start = DateTime.now

    def renderLogPayload(status: String, message: Any): String = {
      Map(
        "id"         -> Cuid.createCuid(),
        "projectId"  -> project.id,
        "functionId" -> function.id,
        "requestId"  -> requestId,
        "status"     -> status,
        "duration"   -> (DateTime.now.getMillis - start.getMillis),
        "timestamp"  -> FunctionExecutor.dateFormatter.print(start),
        "message"    -> message
      ).toJson.compactPrint
    }

    sync(project, function, event)
      .andThen({
        case Success(Bad(FunctionReturnedStringError(error, result))) =>
          logsPublisher.publish(renderLogPayload("FAILURE", Map("event" -> event, "logs" -> result.logs, "returnValue" -> result.returnValue)))

        case Success(Bad(FunctionReturnedJsonError(error, result))) =>
          logsPublisher.publish(renderLogPayload("FAILURE", Map("event" -> event, "logs" -> result.logs, "returnValue" -> result.returnValue)))

        case Success(Bad(FunctionReturnedBadBody(badBody, parseError))) =>
          logsPublisher.publish(renderLogPayload("FAILURE", Map("error" -> s"Couldn't parse response: $badBody. Error message: $parseError")))

        case Success(Bad(FunctionReturnedBadStatus(statusCode, rawResponse))) =>
          logsPublisher.publish(renderLogPayload("FAILURE", Map("error" -> rawResponse))) //Function returned invalid status code: $statusCode. Raw body:

        case Success(Bad(FunctionWebhookURLNotValid(url))) =>
          logsPublisher.publish(renderLogPayload("FAILURE", Map("error" -> s"Function called an invalid url: $url")))

        case Success(Good(FunctionSuccess(values, result))) =>
          logsPublisher.publish(renderLogPayload("SUCCESS", Map("event" -> event, "logs" -> result.logs, "returnValue" -> result.returnValue)))
      })
  }

  def syncWithLoggingAndErrorHandling_!(function: models.Function, event: String, project: Project, requestId: String): Future[FunctionSuccess] = {
    syncWithLogging(function, event, project, requestId) map {
      case Good(x)                                       => x
      case Bad(_: FunctionWebhookURLNotValid)            => throw FunctionWebhookURLWasNotValid(executionId = requestId)
      case Bad(_: FunctionReturnedBadStatus)             => throw UnhandledFunctionError(executionId = requestId)
      case Bad(_: FunctionReturnedBadBody)               => throw FunctionReturnedInvalidBody(executionId = requestId)
      case Bad(FunctionReturnedStringError(errorMsg, _)) => throw FunctionReturnedErrorMessage(errorMsg)
      case Bad(FunctionReturnedJsonError(json, _))       => throw FunctionReturnedErrorObject(json)
    }
  }

  private def handleSuccessfulResponse(project: Project, response: HttpResponse, function: models.Function, acceptEmptyResponse: Boolean)(
      implicit actorSystem: ActorSystem,
      materializer: ActorMaterializer): Future[FunctionSuccess Or FunctionError] = {

    Unmarshal(response).to[String].flatMap { bodyString =>
      handleSuccessfulResponse(project, bodyString, function, acceptEmptyResponse)
    }
  }

  private def handleSuccessfulResponse(project: Project, bodyString: String, function: models.Function, acceptEmptyResponse: Boolean)(
      implicit actorSystem: ActorSystem,
      materializer: ActorMaterializer): Future[FunctionSuccess Or FunctionError] = {

    import cool.graph.util.json.Json._
    import shapeless._
    import syntax.typeable._

    def parseResolverResponse(data: Any, f: FreeType): FunctionDataValue = {
      def tryParsingAsList = Try { data.asInstanceOf[List[Any]].toVector }.getOrElse(throw DataDoesNotMatchPayloadType())

      f.isList match {
        case _ if data == null => FunctionDataValue(isNull = true, Vector.empty)
        case false             => FunctionDataValue(isNull = false, Vector(parseDataToJsObject(data)))
        case true              => FunctionDataValue(isNull = false, tryParsingAsList.map(parseDataToJsObject))
      }
    }

    Future.successful {
      val bodyOrDefault = acceptEmptyResponse match {
        case true  => """{}"""
        case false => bodyString
      }

      bodyOrDefault.tryParseJson.map(myMapFormat.read) match {
        case Success(parsed) =>
          // inline functions are wrapped in {logs:[], response: { this is what we care about }}
          // we should make this handling more explicit
          val functionExecutionResult: FunctionExecutionResult = parsed.get("response") match {
            case Some(response) if response.isInstanceOf[Map[_, _]] =>
              val logs = parsed.get("logs") match {
                case Some(logs) if logs.isInstanceOf[List[_]] => logs.asInstanceOf[List[String]].toVector
                case _                                        => Vector.empty
              }
              FunctionExecutionResult(logs, response.asInstanceOf[Map[String, Any]])

            case None =>
              FunctionExecutionResult(Vector.empty, parsed)
          }

          def getResult(data: Any): FunctionDataValue = function match {
            case f: CustomQueryFunction    => parseResolverResponse(data, f.payloadType)
            case f: CustomMutationFunction => parseResolverResponse(data, f.payloadType)
            case _                         => FunctionDataValue(isNull = false, Vector(parseDataToJsObject(data)))
          }

          def resolverPayloadIsRequired: Boolean = function match {
            case f: CustomQueryFunction    => f.payloadType.isRequired
            case f: CustomMutationFunction => f.payloadType.isRequired
            case _                         => false
          }

          val returnedError: Option[Any]          = functionExecutionResult.returnValue.get("error")
          val stringError: Option[String]         = returnedError.flatMap(e => e.cast[String])
          val jsonError: Option[Map[String, Any]] = returnedError.flatMap(e => e.cast[Map[String, Any]])

          (returnedError, functionExecutionResult.returnValue.get("data")) match {
            case (None, None) if resolverPayloadIsRequired => throw ResolverPayloadIsRequired()
            case (None, None)                              => Good(FunctionSuccess(FunctionDataValue(isNull = true, Vector.empty), functionExecutionResult))
            case (Some(null), Some(data))                  => Good(FunctionSuccess(getResult(data), functionExecutionResult))
            case (None, Some(data))                        => Good(FunctionSuccess(getResult(data), functionExecutionResult))
            case (Some(_), _) if stringError.isDefined     => Bad(FunctionReturnedStringError(stringError.get, functionExecutionResult))
            case (Some(_), _) if jsonError.isDefined       => Bad(FunctionReturnedJsonError(myMapFormat.write(jsonError.get).asJsObject, functionExecutionResult))
            case (Some(error), _)                          => Bad(FunctionReturnedBadBody(bodyString, error.toString))
          }

        case Failure(e) =>
          Bad(FunctionReturnedBadBody(bodyString, e.getMessage))
      }
    }
  }

  private def parseDataToJsObject(data: Any) = {
    Try(data.asInstanceOf[Map[String, Any]].toJson.asJsObject).getOrElse(throw DataDoesNotMatchPayloadType())
  }

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case m: Map[_, _]   => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any]   => JsArray(l.map(write).toVector)
      case l: Vector[Any] => JsArray(l.map(write))
      case n: Int         => JsNumber(n)
      case n: Long        => JsNumber(n)
      case n: BigDecimal  => JsNumber(n)
      case n: Double      => JsNumber(n)
      case n: Float       => JsNumber(n)
      case s: String      => JsString(s)
      case true           => JsTrue
      case false          => JsFalse
      case v: JsValue     => v
      case null           => JsNull
      case r              => JsString(r.toString)
    }

    def read(x: JsValue): Any = {
      x match {
        case l: JsArray   => l.elements.map(read).toList
        case m: JsObject  => m.fields.mapValues(read)
        case s: JsString  => s.value
        case n: JsNumber  => n.value
        case b: JsBoolean => b.value
        case JsNull       => null
        case _            => sys.error("implement all scalar types!")
      }
    }
  }

  implicit lazy val myMapFormat: JsonFormat[Map[String, Any]] = {
    import DefaultJsonProtocol._
    mapFormat[String, Any]
  }
}

object FunctionExecutor {
  import scala.concurrent.duration._

  implicit val marshaller = Conversions.Marshallers.FromString
  implicit val bugsnagger = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  // mysql datetime(3) format
  val dateFormatter              = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
  val defaultRootTokenExpiration = Some(5.minutes.toSeconds)

  def createEventContext(
      project: Project,
      sourceIp: String,
      headers: Map[String, String],
      authenticatedRequest: Option[AuthenticatedRequest],
      endpointResolver: EndpointResolver
  )(implicit inj: Injector): Map[String, Any] = {
    val endpoints = endpointResolver.endpoints(project.id)
    val request = Map(
      "sourceIp"   -> sourceIp,
      "headers"    -> headers,
      "httpMethod" -> "post"
    )

    val tmpRootToken = ClientAuthImpl().generateRootToken("_", project.id, Cuid.createCuid(), defaultRootTokenExpiration)

    val graphcool = Map(
      "projectId" -> project.id,
      "serviceId" -> project.id,
      "alias"     -> project.alias.orNull,
      "pat"       -> tmpRootToken,
      "rootToken" -> tmpRootToken,
      "endpoints" -> endpoints.toMap
    )

    val environment = Map()
    val auth = authenticatedRequest
      .map(authenticatedRequest => {
        val typeName: String = authenticatedRequest match {
          case AuthenticatedUser(_, typeName, _) => typeName
          case AuthenticatedRootToken(_, _)      => "PAT"
          case AuthenticatedCustomer(_, _)       => "Customer"
        }

        Map(
          "nodeId"   -> authenticatedRequest.id,
          "typeName" -> typeName,
          "token"    -> authenticatedRequest.originalToken
        )
      })
      .orNull

    val sessionCache = Map()

    Map(
      "request"      -> request,
      "graphcool"    -> graphcool,
      "environment"  -> environment,
      "auth"         -> auth,
      "sessionCache" -> sessionCache
    )
  }
}
