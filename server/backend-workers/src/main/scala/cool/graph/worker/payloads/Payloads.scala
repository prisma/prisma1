package cool.graph.worker.payloads

import play.api.libs.json.JsObject

case class Webhook(
    projectId: String,
    functionId: String,
    requestId: String,
    url: String,
    payload: String,
    id: String,
    headers: Map[String, String]
)

case class LogItem(
    id: String,
    projectId: String,
    functionId: String,
    requestId: String,
    status: String,
    duration: Long,
    timestamp: String,
    message: JsObject
)
