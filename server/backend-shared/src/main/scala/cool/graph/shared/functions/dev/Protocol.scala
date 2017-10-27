package cool.graph.shared.functions.dev

import play.api.libs.json.{JsObject, Json}

object Conversions {
  implicit val deploymentInputFormat    = Json.format[DeploymentInput]
  implicit val statusResponseFormat     = Json.format[StatusResponse]
  implicit val functionInvocationFormat = Json.format[FunctionInvocation]
  implicit val invacationResultFormat   = Json.format[FunctionInvocationResult]
}

case class DeploymentInput(zipUrl: String, handlerPath: String, functionName: String)
case class StatusResponse(success: Boolean, error: Option[String] = None)
case class FunctionInvocation(functionName: String, input: String)

case class FunctionInvocationResult(
    success: Boolean,
    error: Option[String],
    value: Option[JsObject],
    stdout: Option[String],
    stderr: Option[String]
)
