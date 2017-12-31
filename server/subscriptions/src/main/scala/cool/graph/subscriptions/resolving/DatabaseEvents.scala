package cool.graph.subscriptions.resolving

import play.api.libs.json._

object DatabaseEvents {
  sealed trait DatabaseEvent {
    def nodeId: String
    def modelId: String
  }

  case class DatabaseDeleteEvent(nodeId: String, modelId: String, node: JsObject)                                       extends DatabaseEvent
  case class DatabaseCreateEvent(nodeId: String, modelId: String)                                                       extends DatabaseEvent
  case class DatabaseUpdateEvent(nodeId: String, modelId: String, changedFields: Seq[String], previousValues: JsObject) extends DatabaseEvent

  case class IntermediateUpdateEvent(nodeId: String, modelId: String, changedFields: Seq[String], previousValues: String)

  object DatabaseEventReaders {
    implicit lazy val databaseDeleteEventReads     = Json.reads[DatabaseDeleteEvent]
    implicit lazy val databaseCreateEventReads     = Json.reads[DatabaseCreateEvent]
    implicit lazy val intermediateUpdateEventReads = Json.reads[IntermediateUpdateEvent]

    implicit lazy val databaseUpdateEventReads = new Reads[DatabaseUpdateEvent] {
      override def reads(json: JsValue): JsResult[DatabaseUpdateEvent] = {
        intermediateUpdateEventReads.reads(json) match {
          case x: JsError =>
            x
          case JsSuccess(intermediate, _) =>
            Json.parse(intermediate.previousValues).validate[JsObject] match {
              case x: JsError =>
                x
              case JsSuccess(previousValues, _) =>
                JsSuccess(
                  DatabaseUpdateEvent(
                    intermediate.nodeId,
                    intermediate.modelId,
                    intermediate.changedFields,
                    previousValues
                  ))
            }
        }
      }
    }
  }
}
