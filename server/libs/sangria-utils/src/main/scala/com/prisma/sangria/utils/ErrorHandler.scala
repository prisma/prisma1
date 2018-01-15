package com.prisma.sangria.utils

import akka.http.scaladsl.model.HttpRequest
import com.prisma.errors.{ErrorReporter, GraphQlMetadata, ProjectMetadata, RequestMetadata}
import sangria.execution.{Executor, HandledException}
import sangria.marshalling.ResultMarshaller

trait ErrorWithCode {
  val code: Int
}

case class ErrorHandler(
    requestId: String,
    request: HttpRequest,
    query: String,
    variables: String,
    reporter: ErrorReporter,
    projectId: Option[String] = None
) {
  private val internalErrorMessage = s"Whoops. Looks like an internal server error. Search your cluster logs for request ID: $requestId"

  lazy val handler: PartialFunction[(ResultMarshaller, Throwable), HandledException] = {
    case (marshaller: ResultMarshaller, error: ErrorWithCode) =>
      val additionalFields = Map("code" -> marshaller.scalarNode(error.code, "Int", Set.empty))
      HandledException(error.getMessage, additionalFields ++ commonFields(marshaller))

    case (marshaller, error: Throwable) =>
      error.printStackTrace()

      val requestMetadata = RequestMetadata(requestId, request.method.value, request.uri.toString(), request.headers.map(h => h.name() -> h.value()))
      val graphQlMetadata = GraphQlMetadata(query, variables)
      val projectMetadata = projectId.map(pid => ProjectMetadata(pid))

      reporter.report(error, Seq(requestMetadata, graphQlMetadata) ++ projectMetadata: _*)
      HandledException(internalErrorMessage, commonFields(marshaller))
  }

  lazy val sangriaExceptionHandler: Executor.ExceptionHandler = sangria.execution.ExceptionHandler(
    onException = handler
  )

  private def commonFields(marshaller: ResultMarshaller) = Map(
    "requestId" -> marshaller.scalarNode(requestId, "Int", Set.empty)
  )
}
