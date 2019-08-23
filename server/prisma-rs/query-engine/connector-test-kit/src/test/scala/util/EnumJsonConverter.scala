package util

import play.api.libs.json._

class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends Format[T#Value] {
  override def writes(obj: T#Value): JsValue = JsString(obj.toString)

  override def reads(json: JsValue): JsResult[T#Value] = {
    json match {
      case JsString(str) => JsSuccess(enu.withName(str))
      case _             => JsError(s"$json is not a string and can therefore not be deserialized into an enum")
    }
  }
}
