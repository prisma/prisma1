package cool.graph.deploy.server

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
import cool.graph.deploy.DeployMetrics
import cool.graph.deploy.schema.{SchemaBuilder, SystemUserContext}
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.util.logging.{LogData, LogKey}
import sangria.execution.Executor
import sangria.parser.QueryParser
import scaldi._
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class DeployServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(implicit system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with Injectable
    with LazyLogging {
  import cool.graph.deploy.server.JsonMarshalling._

  import system.dispatcher

  val log: String => Unit = (msg: String) => logger.info(msg)
  val requestPrefix       = "deploy"

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":system:" + createCuid()
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
      TimeResponseDirectiveImpl(DeployMetrics).timeResponse {
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
                  val userContext = SystemUserContext(clientId = "clientId")
                  val result: Future[(StatusCode with Product with Serializable, JsValue)] =
                    Executor
                      .execute(
                        schema = schemaBuilder(userContext),
                        queryAst = queryAst,
                        userContext = userContext,
                        variables = variables,
                        operationName = operationName,
                        middleware = List.empty
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
        getFromResource("graphiql.html")
      }
  }

  def healthCheck: Future[_] = Future.successful(())
}
