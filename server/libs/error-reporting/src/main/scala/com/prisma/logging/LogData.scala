package com.prisma.logging

import play.api.libs.json.{DefaultWrites, JsString, Json, Writes}

object LogKey extends Enumeration {
  val RequestNew      = Value("request/new")
  val RequestQuery    = Value("request/query")
  val RequestComplete = Value("request/complete")
  val UnhandledError  = Value("error/unhandled")
  val HandledError    = Value("error/handled")
}

case class LogData(
    key: LogKey.Value,
    requestId: String,
    clientId: Option[String] = None,
    projectId: Option[String] = None,
    message: Option[String] = None,
    payload: Option[Map[String, Any]] = None
)

object LogDataWrites extends DefaultWrites {
  val anyWrites: Writes[Any] = Writes(any => JsString(any.toString))
  implicit val mapAnyWrites  = mapWrites[Any](anyWrites)
  implicit val logDataWrites = Json.writes[LogData]
}
