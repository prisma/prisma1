package cool.graph.api.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.akkautil.http.Server
import cool.graph.api.database.DataResolver
import cool.graph.api.database.deferreds._
import cool.graph.api.schema.APIErrors.{InvalidToken, ProjectNotFound}
import cool.graph.api.schema.{ApiUserContext, SchemaBuilder, UserFacingError}
import cool.graph.api.{ApiDependencies, ApiMetrics}
import cool.graph.cuid.Cuid.createCuid
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.shared.models.{Project, ProjectId, ProjectWithClientId}
import cool.graph.util.logging.{LogData, LogKey}
import sangria.execution.Executor
import sangria.parser.QueryParser
import scaldi._
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class ApiServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(implicit apiDependencies: ApiDependencies, system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with Injectable
    with LazyLogging {
  import cool.graph.api.server.JsonMarshalling._
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

    post {
      handleExceptions(toplevelExceptionHandler(requestId)) {
        TimeResponseDirectiveImpl(ApiMetrics).timeResponse {
          respondWithHeader(RawHeader("Request-Id", requestId)) {
            pathPrefix(Segment) { name =>
              pathPrefix(Segment) { stage =>
                optionalHeaderValueByName("Authorization") { authorizationHeader =>
                  entity(as[JsValue]) { requestJson =>
                    complete {
                      val projectId = ProjectId.toEncodedString(name = name, stage = stage)
                      fetchProject(projectId).flatMap { project =>
                        verifyAuth(project = project.project, authHeaderOpt = authorizationHeader)
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

                        val dataResolver                                       = DataResolver(project.project)
                        val deferredResolverProvider: DeferredResolverProvider = new DeferredResolverProvider(dataResolver)
                        val masterDataResolver                                 = DataResolver(project.project, useMasterDatabaseOnly = true)

                        QueryParser.parse(query) match {
                          case Failure(error) =>
                            Future.successful(BadRequest -> JsObject("error" -> JsString(error.getMessage)))

                          case Success(queryAst) =>
                            val userContext = ApiUserContext(clientId = "clientId")
                            val result: Future[(StatusCode, JsValue)] =
                              Executor
                                .execute(
                                  schema = schemaBuilder(userContext, project.project, dataResolver, masterDataResolver),
                                  queryAst = queryAst,
                                  userContext = userContext,
                                  variables = variables,
                                  //                        exceptionHandler = ???,
                                  operationName = operationName,
                                  middleware = List.empty,
                                  deferredResolver = deferredResolverProvider
                                )
                                .map(node => OK -> node)

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

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result map {
      case None         => throw ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }

  def verifyAuth(project: Project, authHeaderOpt: Option[String]) = {
    if (project.secrets.isEmpty) {
      ()
    } else {
      authHeaderOpt match {
        case Some(authHeader) => {
          import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}

          val isValid = project.secrets.exists(secret => {
            val jwtOptions = JwtOptions(signature = true, expiration = false)
            val algorithms = Seq(JwtAlgorithm.HS256)
            val claims     = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = secret, algorithms = algorithms, options = jwtOptions)

            // todo: also verify claims in accordance with https://github.com/graphcool/framework/issues/1365

            claims.isSuccess
          })

          if (!isValid) {
            throw InvalidToken()
          }
        }
        case None => throw InvalidToken()
      }
    }
  }

  def healthCheck: Future[_] = Future.successful(())

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: UserFacingError => complete(OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage)))

    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      complete(500 -> s"kaputt: ${e.getMessage}")
  }
}

//object ApiServer {
//  val project = {
//    val schema = SchemaDsl()
//    schema.model("Car").field("wheelCount", _.Int).field_!("name", _.String)
//    schema.buildProject()
//  }
//}
