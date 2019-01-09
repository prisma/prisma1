package com.prisma.sangria.utils

import akka.http.scaladsl.model.HttpRequest
import com.prisma.errors.{ErrorReporter, GraphQlMetadata, ProjectMetadata, RequestMetadata}
import com.prisma.logging.{LogData, LogKey}
import play.api.libs.json.{JsValue, Json}
import sangria.execution.{Executor, HandledException}
import sangria.marshalling.ResultMarshaller

import scala.collection.immutable.Seq

case class ErrorHandler(
    requestId: String,
    method: String,
    uri: String,
    headers: Seq[(String, String)],
    query: String,
    variables: JsValue,
    reporter: ErrorReporter,
    projectId: Option[String] = None,
    errorCodeExtractor: Throwable => Option[Int]
) {
  private val internalErrorMessage = s"Whoops. Looks like an internal server error. Search your server logs for request ID: $requestId"

  lazy val handler: PartialFunction[(ResultMarshaller, Throwable), HandledException] = {
    case (marshaller: ResultMarshaller, error: Throwable) if errorCodeExtractor(error).isDefined =>
      logError(LogKey.HandledError, error, requestId, query, variables.toString, projectId)
      val additionalFields = Map("code" -> marshaller.scalarNode(errorCodeExtractor(error).get, "Int", Set.empty))
      HandledException(error.getMessage, additionalFields ++ commonFields(marshaller))

    case (marshaller, error: Throwable) =>
      val variablesAsString = variables.toString()
      error.printStackTrace()
      logError(LogKey.UnhandledError, error, requestId, query, variablesAsString, projectId)
      val requestMetadata = RequestMetadata(requestId, method, uri, headers)
      val graphQlMetadata = GraphQlMetadata(query, variablesAsString)
      val projectMetadata = projectId.map(pid => ProjectMetadata(pid))
      reporter.report(error, Seq(requestMetadata, graphQlMetadata) ++ projectMetadata: _*)
      HandledException(internalErrorMessage, commonFields(marshaller))
  }

  lazy val sangriaExceptionHandler: Executor.ExceptionHandler = sangria.execution.ExceptionHandler(onException = handler)

  private def commonFields(marshaller: ResultMarshaller) = Map(
    "requestId" -> marshaller.scalarNode(requestId, "Int", Set.empty)
  )

  private def logError(
      logKey: LogKey.Value,
      error: Throwable,
      requestId: String,
      query: String,
      variables: String,
      projectId: Option[String]
  ): Unit = {
    import com.prisma.logging.LogDataWrites.logDataWrites
    val errorCode = error match {
      case error: Throwable if errorCodeExtractor(error).isDefined => errorCodeExtractor(error).get
      case _                                                       => 0
    }
    val payload = Map(
      "message"     -> error.getMessage,
      "exception"   -> error.toString,
      "stack_trace" -> error.getStackTrace.map(_.toString).mkString("\\n "),
      "code"        -> errorCode,
      "query"       -> query,
      "variables"   -> variables
    )

    println(Json.toJson(LogData(logKey, requestId, projectId, payload = Some(payload))))
  }
}
