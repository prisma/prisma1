package com.prisma.api.server

import spray.json.{JsArray, JsNumber, JsObject, JsString}

object JsonErrorHelper {

  def errorJson(requestId: String, message: String, errorCode: Int): JsObject = errorJson(requestId, message, Some(errorCode))
  def errorJson(requestId: String, message: String, errorCode: Option[Int] = None): JsObject = errorCode match {
    case None       => JsObject("errors" -> JsArray(JsObject("message" -> JsString(message), "requestId" -> JsString(requestId))))
    case Some(code) => JsObject("errors" -> JsArray(JsObject("message" -> JsString(message), "code"      -> JsNumber(code), "requestId" -> JsString(requestId))))
  }
}
