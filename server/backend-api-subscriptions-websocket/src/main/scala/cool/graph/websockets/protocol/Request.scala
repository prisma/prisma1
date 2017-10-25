package cool.graph.websockets.protocol

import cool.graph.messagebus.Conversions
import play.api.libs.json.Json

object Request {
  implicit val requestFormat = Json.format[Request]

  implicit val requestUnmarshaller = Conversions.Unmarshallers.ToJsonBackedType[Request]()
  implicit val requestMarshaller   = Conversions.Marshallers.FromJsonBackedType[Request]()
}

case class Request(sessionId: String, projectId: String, body: String)
