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
import cool.graph.akkautil.throttler.Throttler.ThrottlerException
import cool.graph.api.schema.APIErrors.ProjectNotFound
import cool.graph.api.schema.{SchemaBuilder, UserFacingError}
import cool.graph.api.{ApiDependencies, ApiMetrics}
import cool.graph.cuid.Cuid.createCuid
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.shared.models.{ProjectId, ProjectWithClientId}
import cool.graph.util.logging.{LogData, LogKey}
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps

case class ApiServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(implicit apiDependencies: ApiDependencies, system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with LazyLogging {
  import system.dispatcher

  val log: String => Unit = (msg: String) => logger.info(msg)
  val requestPrefix       = "api"
  val projectFetcher      = apiDependencies.projectFetcher

  import scala.concurrent.duration._

  lazy val throttler = Throttler[ProjectId](
    groupBy = pid => pid.name,
    amount = 1,
    per = 5.seconds,
    timeout = 60.seconds,
    maxCallsInFlight = 1
  )

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":api:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: Option[String] = None, clientId: Option[String] = None) = {
      log(
        LogData(
          key = LogKey.RequestComplete,
          requestId = requestId,
          projectId = projectId,
          clientId = clientId,
          payload = Some(Map("request_duration" -> (System.currentTimeMillis() - requestBeginningTime)))
        ).json)
    }

    logger.info(LogData(LogKey.RequestNew, requestId).json)

    pathPrefix(Segment) { name =>
      pathPrefix(Segment) { stage =>
        post {
          handleExceptions(toplevelExceptionHandler(requestId)) {
            path("private") {
              extractRawRequest(requestId) { rawRequest =>
                val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                val result    = apiDependencies.requestHandler.handleRawRequestForPrivateApi(projectId = projectId, rawRequest = rawRequest)
                result.onComplete(_ => logRequestEnd(Some(projectId)))
                complete(result)
              }
            } ~
              path("import") {
                extractRawRequest(requestId) { rawRequest =>
                  val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                  val result    = apiDependencies.requestHandler.handleRawRequestForImport(projectId = projectId, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(Some(projectId)))
                  complete(result)
                }
              } ~
              path("export") {
                extractRawRequest(requestId) { rawRequest =>
                  val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                  val result    = apiDependencies.requestHandler.handleRawRequestForExport(projectId = projectId, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(Some(projectId)))
                  complete(result)
                }
              } ~ {
              extractRawRequest(requestId) { rawRequest =>
                val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                val result = throttler.throttled(ProjectId(name, stage)) { () =>
                  apiDependencies.requestHandler.handleRawRequestForPublicApi(projectId, rawRequest)
                }
                onComplete(result) {
                  case scala.util.Success(result) =>
                    logRequestEnd(Some(projectId))
                    respondWithHeader(RawHeader("Throttled-By", result.throttledBy.toString + "ms")) {
                      complete(result.result)
                    }

                  case scala.util.Failure(_: ThrottlerException) =>
                    logRequestEnd(Some(projectId))
                    complete(OK -> "throttled!")

                  case scala.util.Failure(exception) => // just propagate the exception
                    throw exception
                }
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

  def healthCheck: Future[_] = Future.successful(())

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: UserFacingError =>
      complete(OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage)))

    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      complete(500 -> s"kaputt: ${e.getMessage}")
  }
}
