package cool.graph.webhook

import cool.graph.messagebus.Conversions
import play.api.libs.json.{Json, Reads, Writes}

object Webhook {
  implicit val mapStringReads  = Reads.mapReads[String]
  implicit val mapStringWrites = Writes.mapWrites[String]
  implicit val webhooksWrites  = Json.format[Webhook]
  implicit val marshaller      = Conversions.Marshallers.FromJsonBackedType[Webhook]()
  implicit val unmarshaller    = Conversions.Unmarshallers.ToJsonBackedType[Webhook]()
}

case class Webhook(
    projectId: String,
    functionId: String,
    requestId: String,
    url: String,
    payload: String,
    id: String,
    headers: Map[String, String]
)
