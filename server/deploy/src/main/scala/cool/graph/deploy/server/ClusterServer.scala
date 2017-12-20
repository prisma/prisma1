package cool.graph.deploy.server

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
import cool.graph.cuid.Cuid.createCuid
import cool.graph.deploy.DeployMetrics
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.schema.{DeployApiError, SchemaBuilder, SystemUserContext}
import cool.graph.metrics.extensions.TimeResponseDirectiveImpl
import cool.graph.util.logging.{LogData, LogKey}
import sangria.execution.{Executor, HandledException}
import sangria.marshalling.ResultMarshaller
import sangria.parser.QueryParser
import scaldi._
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class ClusterServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(implicit system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with Injectable
    with LazyLogging {
  import cool.graph.deploy.server.JsonMarshalling._
  import system.dispatcher

  val log: String => Unit = (msg: String) => logger.info(msg)
  val requestPrefix       = "cluster"

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":cluster:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()
    val errorHandler         = ErrorHandler(requestId)

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

    handleExceptions(toplevelExceptionHandler(requestId)) {
      TimeResponseDirectiveImpl(DeployMetrics).timeResponse {
        post {
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
                    val userContext = SystemUserContext()

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
                        .map(node => OK -> node)

                    result.onComplete(_ => logRequestEnd(None, None))
                    result
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

  def healthCheck: Future[_] = Future.successful(())

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: DeployApiError =>
      complete(OK -> JsObject("code" -> JsNumber(e.errorCode), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage)))

    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      complete(500 -> e)
  }
}

case class ErrorHandler(
    requestId: String
) {
  private val internalErrorMessage =
    s"Whoops. Looks like an internal server error. Please contact us from the Console (https://console.graph.cool) or via email (support@graph.cool) and include your Request ID: $requestId"

  lazy val sangriaExceptionHandler: Executor.ExceptionHandler = {
    case (marshaller: ResultMarshaller, error: DeployApiError) =>
      val additionalFields = Map("code" -> marshaller.scalarNode(error.errorCode, "Int", Set.empty))
      HandledException(error.getMessage, additionalFields ++ commonFields(marshaller))

    case (marshaller, error) =>
      error.printStackTrace()
      HandledException(internalErrorMessage, commonFields(marshaller))
  }

  private def commonFields(marshaller: ResultMarshaller) = Map(
    "requestId" -> marshaller.scalarNode(requestId, "Int", Set.empty)
  )
}
