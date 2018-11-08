package com.prisma.deploy.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.Server
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.ProjectPersistence
import com.prisma.deploy.schema.{DeployApiError, InvalidProjectId, SchemaBuilder, SystemUserContext}
import com.prisma.errors.RequestMetadata
import com.prisma.metrics.extensions.TimeResponseDirectiveImpl
import com.prisma.sangria.utils.ErrorHandler
import com.typesafe.scalalogging.LazyLogging
import cool.graph.cuid.Cuid.createCuid
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{Json, _}
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.parser.QueryParser

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class ManagementServer(prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    dependencies: DeployDependencies
) extends Server
    with LazyLogging
    with PlayJsonSupport {
  import com.prisma.deploy.server.JsonMarshalling._
  import system.dispatcher

  val schemaBuilder: SchemaBuilder           = dependencies.managementSchemaBuilder
  val projectPersistence: ProjectPersistence = dependencies.projectPersistence
  val log: String => Unit                    = (msg: String) => logger.info(msg)
  val requestPrefix                          = sys.env.getOrElse("ENV", "local")
  val projectIdEncoder                       = dependencies.projectIdEncoder
  val telemetryActor                         = dependencies.telemetryActor

  def errorExtractor(t: Throwable): Option[Int] = t match {
    case e: DeployApiError => Some(e.code)
    case _                 => None
  }

  val innerRoutes = extractRequest { req =>
    val requestId = requestPrefix + ":management:" + createCuid()
//    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: Option[String] = None, clientId: Option[String] = None) = {
//      log(
//        Json
//          .toJson(
//            LogData(
//              key = LogKey.RequestComplete,
//              requestId = requestId,
//              projectId = projectId,
//              clientId = clientId,
//              payload = Some(Map("request_duration" -> (System.currentTimeMillis() - requestBeginningTime)))
//            )
//          )
//          .toString())
    }

//    logger.info(Json.toJson(LogData(LogKey.RequestNew, requestId)).toString())

    handleExceptions(toplevelExceptionHandler(requestId)) {
      TimeResponseDirectiveImpl.timeResponse {
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
                    case Some(JsString(s)) if s.trim.nonEmpty => Json.parse(s)
                    case _                                    => JsObject.empty
                  }

                  QueryParser.parse(query) match {
                    case Failure(error) =>
                      Future.successful(BadRequest -> Json.obj("error" -> error.getMessage))

                    case Success(queryAst) =>
                      val userContext  = SystemUserContext(authorizationHeader = authorizationHeader)
                      val errorHandler = ErrorHandler(requestId, req, query, variables, dependencies.reporter, errorCodeExtractor = errorExtractor)
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
            }
          }
      }
    }
  }
  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: DeployApiError =>
      complete(OK -> Json.obj("code" -> e.code, "requestId" -> requestId, "error" -> e.getMessage))

    case e: Throwable =>
      extractRequest { req =>
        println(e.getMessage)
        e.printStackTrace()
        dependencies.reporter.report(e, RequestMetadata(requestId, req.method.value, req.uri.toString(), req.headers.map(h => h.name() -> h.value())))
        complete(InternalServerError -> Json.obj("errors" -> Json.arr(Json.obj("requestId" -> requestId), "message" -> e.getMessage)))
      }
  }
}
