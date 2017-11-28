package cool.graph.util.json

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.util.Try

object JsonUtils {
  implicit class JsonStringExtension(val str: String) extends AnyVal {
    def tryParseJson(): Try[JsValue] = Try { play.api.libs.json.Json.parse(str) }
  }

  def enumFormat[T <: scala.Enumeration](enu: T): Format[T#Value] = new EnumJsonConverter[T](enu)

  implicit object DateTimeFormat extends Format[DateTime] {

    val formatter = ISODateTimeFormat.basicDateTime

    def writes(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) =>
        try {
          JsSuccess(formatter.parseDateTime(s))
        } catch {
          case t: Throwable => error(s)
        }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): JsResult[DateTime] = {
      val example = formatter.print(0)
      JsError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }
}

class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends Format[T#Value] {
  override def writes(obj: T#Value): JsValue = JsString(obj.toString)

  override def reads(json: JsValue): JsResult[T#Value] = {
    json match {
      case JsString(str) => JsSuccess(enu.withName(str))
      case _             => JsError(s"$json is not a string and can therefore not be deserialized into an enum")
    }
  }
}
