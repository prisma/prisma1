package cool.graph.system

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph._
import cool.graph.akkautil.http.Server
import cool.graph.cuid.Cuid.createCuid
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.shared.database.{GlobalDatabaseManager, InternalDatabase}
import cool.graph.shared.errors.CommonErrors.TimeoutExceeded
import cool.graph.shared.logging.{LogData, LogKey}
import cool.graph.shared.schema.JsonMarshalling._
import cool.graph.system.authorization.SystemAuth
import cool.graph.system.database.finder.client.ClientResolver
import cool.graph.system.metrics.SystemMetrics
import cool.graph.util.ErrorHandlerFactory
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import scaldi._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class SystemServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(implicit inj: Injector, system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with Injectable
    with LazyLogging {
  import system.dispatcher

  implicit val internalDatabaseDef: DatabaseDef = inject[DatabaseDef](identified by "internal-db")
  implicit val clientResolver                   = inject[ClientResolver](identified by "clientResolver")

  val globalDatabaseManager = inject[GlobalDatabaseManager]
  val internalDatabase      = InternalDatabase(internalDatabaseDef)
  val log: String => Unit   = (msg: String) => logger.info(msg)
  val errorHandlerFactory   = ErrorHandlerFactory(log)
  val requestPrefix         = inject[String](identified by "request-prefix")

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":system:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: Option[String] = None, clientId: Option[String] = None) = {
      logger.info(
        LogData(
          key = LogKey.RequestComplete,
          requestId = requestId,
          projectId = projectId,
          clientId = clientId,
          payload = Some(Map("request_duration" -> (System.currentTimeMillis() - requestBeginningTime)))
        ).json)
    }

    logger.info(LogData(LogKey.RequestNew, requestId).json)

    post {
      TimeResponseDirectiveImpl(SystemMetrics).timeResponse {
        optionalHeaderValueByName("Authorization") { authorizationHeader =>
          optionalCookie("session") { sessionCookie =>
            respondWithHeader(RawHeader("Request-Id", requestId)) {
              entity(as[JsValue]) { requestJson =>
                withRequestTimeoutResponse { request =>
                  val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(requestId = requestId)
                  val error                = TimeoutExceeded()
                  val errorResponse        = unhandledErrorLogger(error)

                  HttpResponse(errorResponse._1, entity = errorResponse._2.prettyPrint)
                } {
                  complete {
                    val JsObject(fields) = requestJson
                    val JsString(query)  = fields("query")

                    val operationName =
                      fields.get("operationName") collect {
                        case JsString(op) if !op.isEmpty ⇒ op
                      }

                    val variables = fields.get("variables") match {
                      case Some(obj: JsObject)                  => obj
                      case Some(JsString(s)) if s.trim.nonEmpty => s.parseJson
                      case _                                    => JsObject.empty
                    }

                    val auth = new SystemAuth()

                    val sessionToken = authorizationHeader
                      .flatMap {
                        case str if str.startsWith("Bearer ") => Some(str.stripPrefix("Bearer "))
                        case _                                => None
                      }
                      .orElse(sessionCookie.map(_.value))

                    val f: Future[SystemUserContext] =
                      sessionToken.flatMap(auth.parseSessionToken) match {
                        case None           => Future.successful(SystemUserContext(None, requestId, logger.info(_)))
                        case Some(clientId) => SystemUserContext.fetchClient(clientId = clientId, requestId = requestId, log = logger.info(_))
                      }

                    f map { userContext =>
                      {
                        QueryParser.parse(query) match {
                          case Failure(error) =>
                            Future.successful(BadRequest -> JsObject("error" -> JsString(error.getMessage)))

                          case Success(queryAst) =>
                            val sangriaErrorHandler = errorHandlerFactory
                              .sangriaHandler(
                                requestId = requestId,
                                query = query,
                                variables = variables,
                                clientId = userContext.client.map(_.id),
                                projectId = None
                              )

                            val result: Future[(StatusCode with Product with Serializable, JsValue)] =
                              Executor
                                .execute(
                                  schema = schemaBuilder(userContext),
                                  queryAst = queryAst,
                                  userContext = userContext,
                                  variables = variables,
                                  exceptionHandler = sangriaErrorHandler,
                                  operationName = operationName,
                                  middleware = List(new FieldMetricsMiddleware)
                                )
                                .map(node => OK -> node)
                                .recover {
                                  case error: QueryAnalysisError => BadRequest          -> error.resolveError
                                  case error: ErrorWithResolver  => InternalServerError -> error.resolveError
                                }

                            result.onComplete(_ => logRequestEnd(None, Some(userContext.clientId)))
                            result
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } ~
      get {
        getFromResource("graphiql.html")
      }
  }

  def healthCheck: Future[_] =
    for {
      _ <- Future.sequence {
            globalDatabaseManager.databases.values.map { db =>
              for {
                _ <- db.master.run(sql"SELECT 1".as[Int])
                _ <- db.readOnly.run(sql"SELECT 1".as[Int])
              } yield ()
            }
          }
    } yield ()
}
