package util

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.util.Try

object JsonFormats {
  implicit object AnyJsonFormat extends Writes[Any] {
    def writes(x: Any): JsValue = x match {
      case m: Map[_, _] => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(writes))
      case l: List[Any] => JsArray(l.map(writes).toVector)
      case n: Int       => JsNumber(n)
      case n: Long      => JsNumber(n)
      case n: Double    => JsNumber(n)
      case s: String    => JsString(s)
      case true         => JsTrue
      case false        => JsFalse
      case v: JsValue   => v
      case null         => JsNull
      case r            => JsString(r.toString)
    }
  }

  implicit object MapJsonWriter extends Writes[Map[String, Any]] {
    override def writes(obj: Map[String, Any]): JsValue = AnyJsonFormat.writes(obj)
  }
}

object JsonUtils extends JsonUtils

trait JsonUtils {
  implicit class JsonStringExtension(val str: String) {
    def tryParseJson(): Try[JsValue] = Try { Json.parse(str) }
    def parseJson(): JsValue         = Json.parse(str)

  }

  implicit class JsValueExtensions(val json: JsValue) {
    def prettyPrint  = Json.prettyPrint(json)
    def compactPrint = Json.stringify(json)

    def asJsObject = json.as[JsObject]
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
