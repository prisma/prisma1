package cool.graph.api.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.akkautil.http.Server
import cool.graph.cuid.Cuid.createCuid
import cool.graph.api.{ApiDependencies, ApiMetrics}
import cool.graph.api.database.{DataResolver}
import cool.graph.api.database.deferreds._
import cool.graph.api.schema.{ApiUserContext, SchemaBuilder}
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.shared.project_dsl.SchemaDsl
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

  val dataResolver                                       = new DataResolver(project = ApiServer.project)
  val deferredResolverProvider: DeferredResolverProvider = new DeferredResolverProvider(dataResolver)

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
      TimeResponseDirectiveImpl(ApiMetrics).timeResponse {
        respondWithHeader(RawHeader("Request-Id", requestId)) {
          entity(as[JsValue]) { requestJson =>
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

              QueryParser.parse(query) match {
                case Failure(error) =>
                  Future.successful(BadRequest -> JsObject("error" -> JsString(error.getMessage)))

                case Success(queryAst) =>
                  val project = ApiServer.project /// we must get ourselves a real project

                  val userContext = ApiUserContext(clientId = "clientId")
                  val result: Future[(StatusCode with Product with Serializable, JsValue)] =
                    Executor
                      .execute(
                        schema = schemaBuilder(userContext, project),
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
    } ~
      get {
        println("lalala")
        getFromResource("graphiql.html")
      }
  }

  def healthCheck: Future[_] = Future.successful(())
}

object ApiServer {
  val project = {
    val schema = SchemaDsl()
    schema.model("Car").field("wheelCount", _.Int).field_!("name", _.String)
    schema.buildProject()
  }
}
