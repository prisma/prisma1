package cool.graph.shared.logging

import cool.graph.JsonFormats
import spray.json.{DefaultJsonProtocol, _}

object LogKey extends Enumeration {
  val RequestNew                   = Value("request/new")
  val RequestQuery                 = Value("request/query")
  val RequestComplete              = Value("request/complete")
  val RequestMetricsFields         = Value("request/metrics/fields")
  val RequestMetricsSql            = Value("request/metrics/sql")
  val RequestMetricsMutactions     = Value("request/metrics/mutactions")
  val UnhandledError               = Value("error/unhandled")
  val HandledError                 = Value("error/handled")
  val MutactionWebhook             = Value("mutaction/webhook")
  val AlgoliaSyncQuery             = Value("mutaction/algoliaSyncQuery")
  val ActionHandlerWebhookComplete = Value("action_handler/webhook/complete")
  val IntegrityViolation           = Value("integrity/violation")
  val RequestProxyBegin            = Value("request/proxy/begin")
  val RequestProxyData             = Value("request/proxy/data")
}

case class LogData(
    key: LogKey.Value,
    requestId: String,
    clientId: Option[String] = None,
    projectId: Option[String] = None,
    message: Option[String] = None,
    payload: Option[Map[String, Any]] = None
) {
  import LogFormats._

  lazy val json: String = this.toJson(logDataFormat).compactPrint
}

object LogFormats extends DefaultJsonProtocol {
  import JsonFormats.AnyJsonFormat

  implicit object LogKeyJsonFormat extends RootJsonFormat[LogKey.Value] {
    def write(obj: LogKey.Value): JsValue = JsString(obj.toString)

    def read(json: JsValue): LogKey.Value = json match {
      case JsString(str) => LogKey.withName(str)
      case _             => throw new DeserializationException("Enum string expected")
    }
  }

  implicit val logDataFormat: RootJsonFormat[LogData] = jsonFormat(LogData, "log_key", "request_id", "client_id", "project_id", "message", "payload")
}
