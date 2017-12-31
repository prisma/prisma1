package cool.graph.api.server

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import cool.graph.api.schema.APIErrors.ClientApiError
import cool.graph.api.schema.UserFacingError
import sangria.execution.{Executor, HandledException}
import sangria.marshalling.ResultMarshaller
import spray.json.{JsNumber, JsObject, JsString}

case class ErrorHandler(
    requestId: String
) {
  private val internalErrorMessage =
    s"Whoops. Looks like an internal server error. Please contact us from the Console (https://console.graph.cool) or via email (support@graph.cool) and include your Request ID: $requestId"

  lazy val handler: PartialFunction[(ResultMarshaller, Throwable), HandledException] = {
    case (marshaller: ResultMarshaller, error: ClientApiError) =>
      val additionalFields = Map("code" -> marshaller.scalarNode(error.code, "Int", Set.empty))
      HandledException(error.getMessage, additionalFields ++ commonFields(marshaller))

    case (marshaller, error: Throwable) =>
      error.printStackTrace()
      HandledException(internalErrorMessage, commonFields(marshaller))
  }

  lazy val sangriaExceptionHandler: Executor.ExceptionHandler = sangria.execution.ExceptionHandler(
    onException = handler
  )

  def handle(throwable: Throwable): (StatusCode, JsObject) = {

    throwable match {
      case e: UserFacingError =>
        OK -> JsObject("code" -> JsNumber(e.code), "requestId" -> JsString(requestId), "error" -> JsString(e.getMessage))

      case e: Throwable =>
        throwable.printStackTrace()
        InternalServerError → JsObject("requestId" -> JsString(requestId), "error" -> JsString(e.getMessage))
    }

  }

  private def commonFields(marshaller: ResultMarshaller) = Map(
    "requestId" -> marshaller.scalarNode(requestId, "Int", Set.empty)
  )
}
