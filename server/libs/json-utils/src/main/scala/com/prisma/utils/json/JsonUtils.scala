package com.prisma.utils.json

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.util.Try

object JsonUtils extends JsonUtils

trait JsonUtils {
  implicit class JsonStringExtension(val str: String) {
    def tryParseJson(): Try[JsValue] = Try { Json.parse(str) }
    def parseJson(): JsValue         = Json.parse(str)

  }

  implicit class JsValueExtensions(val json: JsValue) {
    def prettyPrint  = Json.prettyPrint(json)
    def compactPrint = Json.stringify(json)
  }

  def enumFormat[T <: scala.Enumeration](enu: T): Format[T#Value] = new EnumJsonConverter[T](enu)

  implicit object DateTimeJsonFormat extends Format[DateTime] {

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
