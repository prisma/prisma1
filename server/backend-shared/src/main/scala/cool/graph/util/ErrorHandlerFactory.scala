package cool.graph.util

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ExceptionHandler => AkkaHttpExceptionHandler}
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client.{HandledError, UnhandledError}
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.errors.UserFacingError
import cool.graph.shared.logging.{LogData, LogKey}
import sangria.execution.Executor.{ExceptionHandler => SangriaExceptionHandler}
import sangria.execution._
import sangria.marshalling.MarshallingUtil._
import sangria.marshalling.sprayJson._
import sangria.marshalling.{ResultMarshaller, SimpleResultMarshallerForType}
import scaldi.{Injectable, Injector}
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.ExecutionException

/**
  * Created by sorenbs on 19/07/16.
  */
object ErrorHandlerFactory extends Injectable {

  def internalErrorMessage(requestId: String) =
    s"Whoops. Looks like an internal server error. Please contact us from the Console (https://console.graph.cool) or via email (support@graph.cool) and include your Request ID: $requestId"

  def apply(log: Function[String, Unit])(implicit inj: Injector): ErrorHandlerFactory = {
    val cloudwatch: Cloudwatch = inject[Cloudwatch]("cloudwatch")
    val bugsnagger: BugSnagger = inject[BugSnagger]
    ErrorHandlerFactory(log, cloudwatch, bugsnagger)
  }
}

case class ErrorHandlerFactory(
    log: Function[String, Unit],
    cloudwatch: Cloudwatch,
    bugsnagger: BugSnagger
) {

  type UnhandledErrorLogger = Throwable => (StatusCode, JsObject)

  def sangriaAndUnhandledHandlers(
      requestId: String,
      query: String,
      variables: JsValue,
      clientId: Option[String],
      projectId: Option[String]
  ): (SangriaExceptionHandler, UnhandledErrorLogger) = {
    sangriaHandler(requestId, query, variables, clientId, projectId) -> unhandledErrorHandler(requestId, query, variables, clientId, projectId)
  }

  def sangriaHandler(
      requestId: String,
      query: String,
      variables: JsValue,
      clientId: Option[String],
      projectId: Option[String]
  ): SangriaExceptionHandler = {
    val errorLogger = logError(requestId, query, variables, clientId, projectId)
    val bugsnag     = reportToBugsnag(requestId, query, variables, clientId, projectId)
    val exceptionHandler: SangriaExceptionHandler = {
      case (m: ResultMarshaller, e: UserFacingError) =>
        errorLogger(e, LogKey.HandledError)
        val additionalFields: Seq[(String, m.Node)] =
          Seq("code" -> m.scalarNode(e.code, "Int", Set.empty), "requestId" -> m.scalarNode(requestId, "Int", Set.empty))

        val optionalAdditionalFields = e.functionError.map { functionError =>
          "functionError" -> functionError.convertMarshaled(SimpleResultMarshallerForType(m)) //.convertMarshaled[sangria.ast.AstNode]
        }

        HandledException(e.getMessage, Map(additionalFields ++ optionalAdditionalFields: _*))

      case (m, e: ExecutionException) =>
        e.getCause.printStackTrace()
        errorLogger(e, LogKey.UnhandledError)
        bugsnag(e)
        HandledException(ErrorHandlerFactory.internalErrorMessage(requestId), Map("requestId" -> m.scalarNode(requestId, "Int", Set.empty)))

      case (m, e) =>
        errorLogger(e, LogKey.UnhandledError)
        bugsnag(e)
        HandledException(ErrorHandlerFactory.internalErrorMessage(requestId), Map("requestId" -> m.scalarNode(requestId, "Int", Set.empty)))
    }
    exceptionHandler
  }

  def akkaHttpHandler(
      requestId: String,
      query: String = "unknown",
      variables: JsValue = JsObject.empty,
      clientId: Option[String] = None,
      projectId: Option[String] = None
  ): AkkaHttpExceptionHandler = {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    AkkaHttpExceptionHandler {
      case e: Throwable => complete(unhandledErrorHandler(requestId)(e))
    }
  }

  def unhandledErrorHandler(
      requestId: String,
      query: String = "unknown",
      variables: JsValue = JsObject.empty,
      clientId: Option[String] = None,
      projectId: Option[String] = None
  ): UnhandledErrorLogger = { error: Throwable =>
    val errorLogger = logError(requestId, query, variables, clientId, projectId)
    error match {
      case e: UserFacingError =>
        errorLogger(e, LogKey.HandledError)
        OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(error.getMessage))

      case e =>
        errorLogger(e, LogKey.UnhandledError)
        InternalServerError → JsObject("requestId" -> JsString(requestId), "error" -> JsString(ErrorHandlerFactory.internalErrorMessage(requestId)))
    }
  }

  private def logError(
      requestId: String,
      query: String,
      variables: JsValue,
      clientId: Option[String],
      projectId: Option[String]
  ): (Throwable, LogKey.Value) => Unit = (error: Throwable, logKey: LogKey.Value) => {
    val payload = error match {
      case error: UserFacingError =>
        Map(
          "message"   -> error.getMessage,
          "code"      -> error.code,
          "query"     -> query,
          "variables" -> variables,
          "exception" -> error.toString,
          "stack_trace" -> error.getStackTrace
            .map(_.toString)
            .mkString(", ")
        )
      case error =>
        Map(
          "message"   -> error.getMessage,
          "code"      -> 0,
          "query"     -> query,
          "variables" -> variables,
          "exception" -> error.toString,
          "stack_trace" -> error.getStackTrace
            .map(_.toString)
            .mkString(", ")
        )
    }

    cloudwatch.measure(error match {
      case e: UserFacingError => HandledError(e)
      case e                  => UnhandledError(e)
    })

    log(LogData(logKey, requestId, clientId, projectId, payload = Some(payload)).json)
  }

  private def reportToBugsnag(
      requestId: String,
      query: String,
      variables: JsValue,
      clientId: Option[String],
      projectId: Option[String]
  ): Throwable => Unit = { t: Throwable =>
    val request = GraphCoolRequest(
      requestId = requestId,
      clientId = clientId,
      projectId = projectId,
      query = query,
      variables = variables.prettyPrint
    )
    bugsnagger.report(t, request)
  }
}
