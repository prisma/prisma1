package com.prisma.sangria_server

import play.api.libs.json.{JsObject, Json}

object JsonErrorHelper {

  def errorJson(requestId: String, message: String, errorCode: Int): JsObject = errorJson(requestId, message, Some(errorCode))
  def errorJson(requestId: String, message: String, errorCode: Option[Int] = None): JsObject = errorCode match {
    case None       => Json.obj("errors" -> Seq(Json.obj("message" -> message, "requestId" -> requestId)))
    case Some(code) => Json.obj("errors" -> Seq(Json.obj("message" -> message, "code"      -> code, "requestId" -> requestId)))
  }
}
