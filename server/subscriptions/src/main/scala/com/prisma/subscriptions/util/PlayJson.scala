package cool.graph.subscriptions.util

import play.api.libs.json._

object PlayJson {
  def parse(str: String): JsResult[JsValue] = {
    try {
      JsSuccess(Json.parse(str))
    } catch {
      case _: Exception =>
        JsError(s"The provided string does not represent valid JSON. The string was: $str")
    }
  }

  def parse(bytes: Array[Byte]): JsResult[JsValue] = {
    try {
      JsSuccess(Json.parse(bytes))
    } catch {
      case _: Exception =>
        JsError(s"The provided byte array does not represent valid JSON.")
    }
  }
}
