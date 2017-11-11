package cool.graph.worker.payloads

import cool.graph.messagebus.Conversions
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller}
import play.api.libs.json._

object JsonConversions {

  implicit val mapStringReads: Reads[Map[String, String]]               = Reads.mapReads[String]
  implicit val mapStringWrites: OWrites[collection.Map[String, String]] = Writes.mapWrites[String]

  implicit val webhookFormat: OFormat[Webhook] = Json.format[Webhook]
  implicit val logItemFormat: OFormat[LogItem] = Json.format[LogItem]

  implicit val webhookMarshaller: ByteMarshaller[Webhook]     = Conversions.Marshallers.FromJsonBackedType[Webhook]()
  implicit val webhookUnmarshaller: ByteUnmarshaller[Webhook] = Conversions.Unmarshallers.ToJsonBackedType[Webhook]()

  implicit val logItemUnmarshaller: ByteUnmarshaller[LogItem] = Conversions.Unmarshallers.ToJsonBackedType[LogItem]()
  implicit val logItemMarshaller: ByteMarshaller[LogItem]     = Conversions.Marshallers.FromJsonBackedType[LogItem]()
}
