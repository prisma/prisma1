package cool.graph.api.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.akkautil.http.Server
import cool.graph.akkautil.throttler.Throttler
import cool.graph.akkautil.throttler.Throttler.ThrottleBufferFullException
import cool.graph.api.schema.APIErrors.ProjectNotFound
import cool.graph.api.schema.CommonErrors.ThrottlerBufferFullException
import cool.graph.api.schema.{SchemaBuilder, UserFacingError}
import cool.graph.api.{ApiDependencies, ApiMetrics}
import cool.graph.cuid.Cuid.createCuid
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.shared.models.{ProjectId, ProjectWithClientId}
import com.prisma.logging.{LogData, LogKey}
import com.prisma.logging.LogDataWrites.logDataWrites
import play.api.libs.json.Json
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps

case class ApiServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(
    implicit apiDependencies: ApiDependencies,
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Server
    with LazyLogging {
  import system.dispatcher

  val log: String => Unit = (msg: String) => logger.info(msg)
  val requestPrefix       = "api"
  val projectFetcher      = apiDependencies.projectFetcher

  import scala.concurrent.duration._

  lazy val unthrottledProjectIds = sys.env.get("UNTHROTTLED_PROJECT_IDS") match {
    case Some(envValue) => envValue.split('|').toVector.map(ProjectId.fromEncodedString)
    case None           => Vector.empty
  }

  lazy val throttler: Option[Throttler[ProjectId]] = {
    for {
      throttlingRate    <- sys.env.get("THROTTLING_RATE")
      maxCallsInFlights <- sys.env.get("THROTTLING_MAX_CALLS_IN_FLIGHT")
    } yield {
      Throttler[ProjectId](
        groupBy = pid => pid.name + "_" + pid.stage,
        amount = throttlingRate.toInt,
        per = 1.seconds,
        timeout = 25.seconds,
        maxCallsInFlight = maxCallsInFlights.toInt
      )
    }
  }

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":api:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: String, throttledBy: Long = 0) = {
      val end            = System.currentTimeMillis()
      val actualDuration = end - requestBeginningTime - throttledBy
      ApiMetrics.requestDuration.record(actualDuration, Seq(projectId))
      log(
        Json
          .toJson(
            LogData(
              key = LogKey.RequestComplete,
              requestId = requestId,
              projectId = Some(projectId),
              payload = Some(
                Map(
                  "request_duration" -> (end - requestBeginningTime),
                  "throttled_by"     -> throttledBy
                ))
            )
          )
          .toString())
    }

    def throttleApiCallIfNeeded(name: String, stage: String, rawRequest: RawRequest) = {
      val projectId = ProjectId(name = name, stage = stage)
      throttler match {
        case Some(throttler) if !unthrottledProjectIds.contains(projectId) => throttledCall(name, stage, rawRequest, throttler)
        case _                                                             => unthrottledCall(name, stage, rawRequest)
      }
    }

    def unthrottledCall(name: String, stage: String, rawRequest: RawRequest) = {
      val projectId = ProjectId.toEncodedString(name = name, stage = stage)
      val result    = apiDependencies.requestHandler.handleRawRequestForPublicApi(projectId, rawRequest)
      complete(result)
    }

    def throttledCall(name: String, stage: String, rawRequest: RawRequest, throttler: Throttler[ProjectId]) = {
      val projectId = ProjectId.toEncodedString(name = name, stage = stage)
      val result = throttler.throttled(ProjectId(name, stage)) { () =>
        apiDependencies.requestHandler.handleRawRequestForPublicApi(projectId, rawRequest)
      }
      onComplete(result) {
        case scala.util.Success(result) =>
          logRequestEnd(projectId)
          respondWithHeader(RawHeader("Throttled-By", result.throttledBy.toString + "ms")) {
            complete(result.result)
          }

        case scala.util.Failure(_: ThrottleBufferFullException) =>
          logRequestEnd(projectId)
          throw ThrottlerBufferFullException()

        case scala.util.Failure(exception) => // just propagate the exception
          logRequestEnd(projectId)
          throw exception
      }
    }

    logger.info(Json.toJson(LogData(LogKey.RequestNew, requestId)).toString())

    pathPrefix(Segment) { name =>
      pathPrefix(Segment) { stage =>
        post {
          handleExceptions(toplevelExceptionHandler(requestId)) {
            path("private") {
              extractRawRequest(requestId) { rawRequest =>
                val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                val result    = apiDependencies.requestHandler.handleRawRequestForPrivateApi(projectId = projectId, rawRequest = rawRequest)
                result.onComplete(_ => logRequestEnd(projectId))
                complete(result)
              }
            } ~
              path("import") {
                extractRawRequest(requestId) { rawRequest =>
                  val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                  val result    = apiDependencies.requestHandler.handleRawRequestForImport(projectId = projectId, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(projectId))
                  complete(result)
                }
              } ~
              path("export") {
                extractRawRequest(requestId) { rawRequest =>
                  val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                  val result    = apiDependencies.requestHandler.handleRawRequestForExport(projectId = projectId, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(projectId))
                  complete(result)
                }
              } ~ {
              extractRawRequest(requestId) { rawRequest =>
                throttleApiCallIfNeeded(name, stage, rawRequest)
              }
            }
          }
        } ~ get {
          getFromResource("graphiql.html")
        }
      }
    }
  }

  def extractRawRequest(requestId: String)(fn: RawRequest => Route): Route = {
    optionalHeaderValueByName("Authorization") { authorizationHeader =>
      TimeResponseDirectiveImpl(ApiMetrics).timeResponse {
        optionalHeaderValueByName("x-graphcool-source") { graphcoolSourceHeader =>
          entity(as[JsValue]) { requestJson =>
            extractClientIP { clientIp =>
              respondWithHeader(RawHeader("Request-Id", requestId)) {
                fn(
                  RawRequest(
                    id = requestId,
                    json = requestJson,
                    ip = clientIp.toString,
                    sourceHeader = graphcoolSourceHeader,
                    authorizationHeader = authorizationHeader
                  )
                )
              }
            }
          }
        }
      }
    }
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result map {
      case None         => throw ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: UserFacingError =>
      complete(OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage)))

    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      apiDependencies.reporter.report(e)
      complete(InternalServerError -> JsObject("errors" -> JsArray(JsObject("requestId" -> JsString(requestId), "message" -> JsString(e.getMessage)))))
  }
}
