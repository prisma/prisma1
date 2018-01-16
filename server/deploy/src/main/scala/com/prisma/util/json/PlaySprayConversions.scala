package cool.graph.util.json

import play.api.libs.json.{
  JsArray => PJsArray,
  JsBoolean => PJsBoolean,
  JsNull => PJsNull,
  JsNumber => PJsNumber,
  JsObject => PJsObject,
  JsString => PJsString,
  JsValue => PJsValue
}
import spray.json._

object PlaySprayConversions extends PlaySprayConversions

trait PlaySprayConversions {

  implicit class PlayToSprayExtension(jsValue: PJsValue) {
    def toSpray(): JsValue = toSprayImpl(jsValue)
  }

  implicit class SprayToPlayExtension(jsValue: JsValue) {
    def toPlay(): PJsValue = toPlayImpl(jsValue)
  }

  private def toSprayImpl(jsValue: PJsValue): JsValue = {
    jsValue match {
      case PJsObject(fields)  => JsObject(fields.map { case (name, jsValue) => (name, toSprayImpl(jsValue)) }.toMap)
      case PJsArray(elements) => JsArray(elements.map(toSprayImpl).toVector)
      case PJsString(s)       => JsString(s)
      case PJsNumber(nr)      => JsNumber(nr)
      case PJsBoolean(b)      => JsBoolean(b)
      case PJsNull            => JsNull
    }
  }

  private def toPlayImpl(jsValue: JsValue): PJsValue = {
    jsValue match {
      case JsObject(fields)  => PJsObject(fields.mapValues(toPlayImpl).toSeq)
      case JsArray(elements) => PJsArray(elements.map(toPlayImpl))
      case JsString(s)       => PJsString(s)
      case JsNumber(nr)      => PJsNumber(nr)
      case JsBoolean(b)      => PJsBoolean(b)
      case JsNull            => PJsNull
    }
  }
}
