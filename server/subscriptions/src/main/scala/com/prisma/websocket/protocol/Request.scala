package com.prisma.websocket.protocol

import com.prisma.messagebus.Conversions
import play.api.libs.json.Json

object Request {
  implicit val requestFormat = Json.format[Request]

  implicit val requestUnmarshaller = Conversions.Unmarshallers.ToJsonBackedType[Request]()
  implicit val requestMarshaller   = Conversions.Marshallers.FromJsonBackedType[Request]()
}

case class Request(sessionId: String, projectId: String, body: String)
