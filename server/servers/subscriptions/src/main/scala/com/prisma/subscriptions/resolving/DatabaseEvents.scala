package com.prisma.subscriptions.resolving

import com.prisma.gc_values.{StringIdGCValue, IdGCValue, IntGCValue, UuidGCValue}
import play.api.libs.json._

import scala.util.Success

object DatabaseEvents {
  sealed trait DatabaseEvent {
    def nodeId: IdGCValue
    def modelId: String
  }

  case class DatabaseDeleteEvent(nodeId: IdGCValue, modelId: String, node: JsObject)                                       extends DatabaseEvent
  case class DatabaseCreateEvent(nodeId: IdGCValue, modelId: String)                                                       extends DatabaseEvent
  case class DatabaseUpdateEvent(nodeId: IdGCValue, modelId: String, changedFields: Seq[String], previousValues: JsObject) extends DatabaseEvent

  object DatabaseEventReaders {
    implicit val idReads = new Reads[IdGCValue] {
      override def reads(json: JsValue): JsResult[IdGCValue] = {
        val result = json match {
          case id: JsNumber => IntGCValue(id.value.toInt)
          case id: JsString => stringToIdGcValue(id.value)
          case x            => sys.error("An id should always be of type JsNumber or JsString. " + x)
        }
        JsSuccess(result)
      }

      private def stringToIdGcValue(str: String): IdGCValue = {
        UuidGCValue.parse(str) match {
          case Success(id) => id
          case _           => StringIdGCValue(str)
        }
      }
    }

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
                (parsed \ "nodeId").as[IdGCValue],
                (parsed \ "modelId").as[String],
                (parsed \ "changedFields").as[Seq[String]],
                (parsed \ "previousValues").asOpt[JsObject].getOrElse(JsObject.empty)
              ))
        }
      }
    }
  }
}
