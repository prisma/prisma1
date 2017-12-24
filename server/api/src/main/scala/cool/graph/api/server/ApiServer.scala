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
                val result    = apiDependencies.requestHandler.handleRawRequest(projectId, rawRequest)
                result.onComplete(_ => logRequestEnd(Some(projectId)))
                complete(result)
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
