package cool.graph.util.logging

import play.api.libs.json.{DefaultWrites, JsString, Json, Writes}

object LogKey extends Enumeration {
  val RequestNew      = Value("request/new")
  val RequestQuery    = Value("request/query")
  val RequestComplete = Value("request/complete")

  val UnhandledError = Value("error/unhandled")
  val HandledError   = Value("error/handled")
}

case class LogData(
    key: LogKey.Value,
    requestId: String,
    projectId: Option[String] = None,
    payload: Option[Map[String, Any]] = None
)

object LogDataWrites extends DefaultWrites {
  implicit val anyWrites: Writes[Any] = Writes(any => JsString(any.toString))
  implicit val logDataWrites          = Json.writes[LogData]
}
