package cool.graph.localfaas

import play.api.libs.json.{JsObject, Json}

object Conversions {
  implicit val deploymentInputFormat    = Json.format[DeploymentInput]
  implicit val statusResponseFormat     = Json.format[StatusResponse]
  implicit val functionInvocationFormat = Json.format[FunctionInvocation]
  implicit val invocationResultFormat   = Json.format[FunctionInvocationResult]
}

case class DeploymentInput(zipUrl: String, handlerPath: String, functionName: String)
case class StatusResponse(success: Boolean, error: Option[String] = None)
case class FunctionInvocation(functionName: String, input: String)

// PARSE the stdout and then fill the fields!
case class FunctionInvocationResult(
    success: Option[Boolean],
    error: Option[String],
    value: Option[JsObject],
    stdout: String,
    stderr: String
) {
  def printSummary(duration: Long, success: Boolean, projectId: String, name: String): Unit = {
    println(
      s"""Function invocation summary for project $projectId and function $name:
         |\tDuration: ${duration}ms
         |\tSuccess: $success
         |\tFunction return value: '${value.getOrElse("")}'
         |\tError: '${error.getOrElse("").stripLineEnd.trim}'
         |\tProcess stdout: '${stdout.stripLineEnd.trim}'
         |\tProcess stderr: '${stderr.stripLineEnd.trim}'
       """.stripMargin
    )
  }
}
