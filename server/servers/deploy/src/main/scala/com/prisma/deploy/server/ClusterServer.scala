package com.prisma.deploy.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.Server
import com.prisma.deploy.connector.ProjectPersistence
import com.prisma.deploy.schema.{DeployApiError, InvalidProjectId, SchemaBuilder, SystemUserContext}
import com.prisma.deploy.{DeployDependencies, DeployMetrics}
import com.prisma.errors.RequestMetadata
import com.prisma.logging.LogDataWrites.logDataWrites
import com.prisma.logging.{LogData, LogKey}
import com.prisma.metrics.extensions.TimeResponseDirectiveImpl
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.models.ProjectWithClientId
import com.typesafe.scalalogging.LazyLogging
import cool.graph.cuid.Cuid.createCuid
import play.api.libs.json.Json
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class ClusterServer(prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    dependencies: DeployDependencies
) extends Server
    with LazyLogging {
  import com.prisma.deploy.server.JsonMarshalling._
  import system.dispatcher

  val schemaBuilder: SchemaBuilder           = dependencies.clusterSchemaBuilder
  val projectPersistence: ProjectPersistence = dependencies.projectPersistence
  val log: String => Unit                    = (msg: String) => logger.info(msg)
  val requestPrefix                          = "cluster"
  val server2serverSecret                    = sys.env.getOrElse("SCHEMA_MANAGER_SECRET", sys.error("SCHEMA_MANAGER_SECRET env var required but not found"))

  def errorExtractor(t: Throwable): Option[Int] = t match {
    case e: DeployApiError => Some(e.code)
    case _                 => None
  }

  val innerRoutes = extractRequest { req =>
    val requestId            = requestPrefix + ":cluster:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: Option[String] = None, clientId: Option[String] = None) = {
      log(
        Json
          .toJson(
            LogData(
              key = LogKey.RequestComplete,
              requestId = requestId,
              projectId = projectId,
              clientId = clientId,
              payload = Some(Map("request_duration" -> (System.currentTimeMillis() - requestBeginningTime)))
            )
          )
          .toString())
    }

    logger.info(Json.toJson(LogData(LogKey.RequestNew, requestId)).toString())

    handleExceptions(toplevelExceptionHandler(requestId)) {
      TimeResponseDirectiveImpl(DeployMetrics).timeResponse {
        post {
          optionalHeaderValueByName("Authorization") { authorizationHeader =>
            respondWithHeader(RawHeader("Request-Id", requestId)) {
              entity(as[JsValue]) { requestJson =>
                complete {
                  val JsObject(fields) = requestJson
                  val JsString(query)  = fields("query")

                  val operationName =
                    fields.get("operationName") collect {
                      case JsString(op) if !op.isEmpty => op
                    }

                  val variables = fields.get("variables") match {
                    case Some(obj: JsObject)                  => obj
                    case Some(JsString(s)) if s.trim.nonEmpty => s.parseJson
                    case _                                    => JsObject.empty
                  }

                  QueryParser.parse(query) match {
                    case Failure(error) =>
                      Future.successful(BadRequest -> JsObject("error" -> JsString(error.getMessage)))

                    case Success(queryAst) =>
                      val userContext  = SystemUserContext(authorizationHeader = authorizationHeader)
                      val errorHandler = ErrorHandler(requestId, req, query, variables.toString(), dependencies.reporter, errorCodeExtractor = errorExtractor)
                      val result: Future[(StatusCode, JsValue)] =
                        Executor
                          .execute(
                            schema = schemaBuilder(userContext),
                            queryAst = queryAst,
                            userContext = userContext,
                            variables = variables,
                            operationName = operationName,
                            middleware = List.empty,
                            exceptionHandler = errorHandler.sangriaExceptionHandler
                          )
                          .recover {
                            case e: QueryAnalysisError => e.resolveError
                          }
                          .map(node => OK -> node)

                      result.onComplete(_ => logRequestEnd(None, None))
                      result
                  }
                }
              }
            }
          }
        } ~
          get {
            pathEnd {
              getFromResource("graphiql.html")
            } ~ pathPrefix("schema") {
              pathPrefix(Segment) { projectId =>
                optionalHeaderValueByName("Authorization") {
                  case Some(authorizationHeader) if authorizationHeader == s"Bearer $server2serverSecret" =>
                    parameters('forceRefresh ? false) { forceRefresh =>
                      complete(performSchemaRequest(projectId, forceRefresh, logRequestEnd))
                    }

                  case Some(h) =>
                    println(s"Wrong Authorization Header supplied: '$h'")
                    complete(Unauthorized -> "Wrong Authorization Header supplied")

                  case None =>
                    println("No Authorization Header supplied")
                    complete(Unauthorized -> "No Authorization Header supplied")
                }
              }
            }
          }
      }
    }
  }

  def performSchemaRequest(projectId: String, forceRefresh: Boolean, requestEnd: (Option[String], Option[String]) => Unit) = {
    getSchema(projectId, forceRefresh)
      .map(res => OK -> res)
      .andThen {
        case _ => requestEnd(Some(projectId), None)
      }
      .recover {
        case error: Throwable => BadRequest -> error.toString
      }
  }

  def getSchema(projectId: String, forceRefresh: Boolean): Future[String] = {
    import com.prisma.shared.models.ProjectJsonFormatter._
    projectPersistence
      .load(projectId)
      .flatMap {
        case None    => Future.failed(InvalidProjectId(projectId))
        case Some(p) => Future.successful(Json.toJson(ProjectWithClientId(p, p.ownerId)).toString)
      }
  }

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: DeployApiError =>
      complete(OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage)))

    case e: Throwable =>
      extractRequest { req =>
        println(e.getMessage)
        e.printStackTrace()
        dependencies.reporter.report(e, RequestMetadata(requestId, req.method.value, req.uri.toString(), req.headers.map(h => h.name() -> h.value())))
        complete(InternalServerError -> JsObject("errors" -> JsArray(JsObject("requestId" -> JsString(requestId), "message" -> JsString(e.getMessage)))))
      }
  }
}
