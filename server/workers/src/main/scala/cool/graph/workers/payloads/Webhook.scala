package cool.graph.workers.payloads

case class Webhook(
    projectId: String,
    functionId: String,
    requestId: String,
    url: String,
    payload: String,
    id: String,
    headers: Map[String, String]
)
