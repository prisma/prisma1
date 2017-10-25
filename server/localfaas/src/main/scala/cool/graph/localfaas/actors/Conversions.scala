package cool.graph.localfaas.actors

import cool.graph.localfaas.actors.MappingActor.HandlerMap
import play.api.libs.json._

import scala.collection.mutable

object Conversions {
  implicit val mapStringReads  = Reads.mapReads[String]
  implicit val mapStringWrites = Writes.mapWrites[String]

  implicit val mapMapStringReads  = Reads.mapReads[Map[String, String]]
  implicit val mapMapStringWrites = Writes.mapWrites[Map[String, String]]

  implicit val mapReads: Reads[HandlerMap] = new Reads[HandlerMap] {
    def reads(jv: JsValue): JsResult[HandlerMap] = {
      val result = new HandlerMap

      jv.as[Map[String, Map[String, String]]].map {
        case (k: String, v) =>
          val innerMap = new mutable.HashMap[String, String]()
          v.foreach(entry => innerMap += (entry._1 -> entry._2))
          result += k -> innerMap
      }

      JsSuccess(result)
    }
  }

  implicit val mapWrites: Writes[HandlerMap] = new Writes[HandlerMap] {
    def writes(handlers: HandlerMap): JsValue = {
      val entries = handlers.toMap.map {
        case (k, v) => k -> Map(v.toSeq: _*)
      }

      Json.toJson(entries)
    }
  }
}
