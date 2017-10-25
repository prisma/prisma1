package cool.graph.util.json

import spray.json._

import scala.util.{Failure, Success, Try}
import cool.graph.util.exceptions.ExceptionStacktraceToString._

object Json extends SprayJsonExtensions {

  /**
    * extracts a nested json value by a given path like "foo.bar.fizz"
    */
  def getPathAs[T <: JsValue](json: JsValue, path: String): T = {
    def getArrayIndex(pathElement: String): Option[Int] = Try(pathElement.replaceAllLiterally("[", "").replaceAllLiterally("]", "").toInt).toOption

    def getPathAsInternal[T <: JsValue](json: JsValue, pathElements: Seq[String]): Try[T] = {
      if (pathElements.isEmpty) {
        Try(json.asInstanceOf[T])
      } else if (getArrayIndex(pathElements.head).isDefined) {
        Try(json.asInstanceOf[JsArray]) match {
          case Success(jsList) =>
            val index = getArrayIndex(pathElements.head).get
            val subJson = jsList.elements
              .lift(index)
              .getOrElse(sys.error(s"Could not find pathElement [${pathElements.head} in this json $json]"))
            getPathAsInternal(subJson, pathElements.tail)
          case Failure(e) => Failure(e) //sys.error(s"[$json] is not a Jsbject!")
        }
      } else {
        Try(json.asJsObject) match {
          case Success(jsObject) =>
            val subJson = jsObject.fields.getOrElse(pathElements.head, sys.error(s"Could not find pathElement [${pathElements.head} in this json $json]"))
            getPathAsInternal(subJson, pathElements.tail)
          case Failure(e) => Failure(e) //sys.error(s"[$json] is not a Jsbject!")
        }
      }
    }
    getPathAsInternal[T](json, path.split('.')) match {
      case Success(x) =>
        x
      case Failure(e) =>
        sys.error(s"Getting the path $path in $json failed with the following error: ${e.stackTraceAsString}")
    }
  }

  def getPathAs[T <: JsValue](jsonString: String, path: String): T = {
    import spray.json._
    getPathAs(jsonString.parseJson, path)
  }

}

trait SprayJsonExtensions {
  implicit class StringExtensions(string: String) {
    def tryParseJson(): Try[JsValue] = Try { string.parseJson }
  }

  implicit class JsValueParsingExtensions(jsValue: JsValue) {
    def pathAs[T <: JsValue](path: String): T = Json.getPathAs[T](jsValue, path)

    def pathAsJsValue(path: String): JsValue   = pathAs[JsValue](path)
    def pathAsJsObject(path: String): JsObject = pathAs[JsObject](path)
    def pathExists(path: String): Boolean      = Try(pathAsJsValue(path)).map(_ => true).getOrElse(false)

    def pathAsSeq(path: String): Seq[JsValue] = Json.getPathAs[JsArray](jsValue, path).elements
    def pathAsSeqOfType[T](path: String)(implicit format: JsonFormat[T]): Seq[T] =
      Json.getPathAs[JsArray](jsValue, path).elements.map(_.convertTo[T])

    def pathAsString(path: String): String = {
      try {
        pathAs[JsString](path).value
      } catch {
        case e: Exception =>
          pathAs[JsNull.type](path)
          null
      }
    }

    def pathAsLong(path: String): Long = pathAs[JsNumber](path).value.toLong

    def pathAsFloat(path: String): Float = pathAs[JsNumber](path).value.toFloat

    def pathAsDouble(path: String): Double = pathAs[JsNumber](path).value.toDouble

    def pathAsBool(path: String): Boolean = pathAs[JsBoolean](path).value

    def getFirstErrorMessage = jsValue.pathAsSeq("errors").head.pathAsString("message")

    def getFirstErrorCode = jsValue.pathAsSeq("errors").head.pathAsLong("code")

    def getFirstFunctionErrorMessage = jsValue.pathAsSeq("errors").head.pathAsString("functionError")
  }

}
