package cool.graph.shared.algolia

import spray.json._

object AlgoliaEventJsonProtocol extends DefaultJsonProtocol {
  implicit val eventFormat: RootJsonFormat[AlgoliaEvent] = jsonFormat7(AlgoliaEvent)
}

case class AlgoliaEvent(
    indexName: String,
    applicationId: String,
    apiKey: String,
    operation: String,
    nodeId: String,
    requestId: String,
    queryResult: String
)
