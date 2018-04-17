package com.prisma.subscriptions.resolving

import play.api.libs.json._

object DatabaseEvents {
  sealed trait DatabaseEvent {
    def nodeId: String
    def modelId: String
  }

  case class DatabaseDeleteEvent(nodeId: String, modelId: String, node: JsObject)                                       extends DatabaseEvent
  case class DatabaseCreateEvent(nodeId: String, modelId: String)                                                       extends DatabaseEvent
  case class DatabaseUpdateEvent(nodeId: String, modelId: String, changedFields: Seq[String], previousValues: JsObject) extends DatabaseEvent

  object DatabaseEventReaders {
    implicit lazy val databaseDeleteEventReads = Json.reads[DatabaseDeleteEvent]
    implicit lazy val databaseCreateEventReads = Json.reads[DatabaseCreateEvent]
    implicit lazy val databaseUpdateEventReads = new Reads[DatabaseUpdateEvent] {
      override def reads(js: JsValue) = {
        js.validate[JsObject] match {
          case x: JsError =>
            x

          case JsSuccess(parsed, _) =>
            JsSuccess(
              DatabaseUpdateEvent(
                (parsed \ "nodeId").as[String],
                (parsed \ "modelId").as[String],
                (parsed \ "changedFields").as[Seq[String]],
                (parsed \ "previousValues").asOpt[JsObject].getOrElse(JsObject.empty)
              ))
        }
      }
    }
  }
}
