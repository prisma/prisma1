package util

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object PlayJson extends PlayJsonExtensions with JsonUtils {

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
            val subJson = jsList.value
              .lift(index)
              .getOrElse(sys.error(s"Could not find pathElement [${pathElements.head} in this json $json]"))
            getPathAsInternal(subJson, pathElements.tail)
          case Failure(e) => Failure(e) //sys.error(s"[$json] is not a JsObject!")
        }
      } else {
        Try(json.asJsObject) match {
          case Success(jsObject) =>
            val subJson = jsObject.value.getOrElse(pathElements.head, sys.error(s"Could not find pathElement [${pathElements.head} in this json $json]"))
            getPathAsInternal(subJson, pathElements.tail)
          case Failure(e) => Failure(e) //sys.error(s"[$json] is not a JsObject!")
        }
      }
    }
    getPathAsInternal[T](json, path.split('.')) match {
      case Success(x) =>
        x
      case Failure(e) =>
        val stackTraceAsString = e.getStackTrace.map(_.toString).mkString(",")
        sys.error(s"Getting the path $path in $json failed with the following error: ${stackTraceAsString}")
    }
  }

  def getPathAs[T <: JsValue](jsonString: String, path: String): T = getPathAs(jsonString.parseJson, path)
}

trait PlayJsonExtensions extends JsonUtils {
  implicit class StringExtensions(string: String) {
    def tryParseJson(): Try[JsValue] = Try { string.parseJson }
  }

  implicit class JsValueParsingExtensions(jsValue: JsValue) {
    def pathAs[T <: JsValue](path: String): T = PlayJson.getPathAs[T](jsValue, path)

    def pathAsJsValue(path: String): JsValue = pathAs[JsValue](path)

    def pathAsJsObject(path: String): JsObject = pathAs[JsObject](path)

    def pathAsJsArray(path: String): JsArray = pathAs[JsArray](path)

    def pathExists(path: String): Boolean = Try(pathAsJsValue(path)).map(_ => true).getOrElse(false)

    def pathAsSeq(path: String): Seq[JsValue] = PlayJson.getPathAs[JsArray](jsValue, path).value

    def pathAsSeqOfType[T](path: String)(implicit format: Format[T]): Seq[T] = PlayJson.getPathAs[JsArray](jsValue, path).value.map(_.as[T])

    def pathAsString(path: String): String = pathAs[JsString](path).value

    def pathAsLong(path: String): Long = pathAs[JsNumber](path).value.toLong

    def pathAsFloat(path: String): Float = pathAs[JsNumber](path).value.toFloat

    def pathAsDouble(path: String): Double = pathAs[JsNumber](path).value.toDouble

    def pathAsBool(path: String): Boolean = pathAs[JsBoolean](path).value

    def getFirstErrorMessage = jsValue.pathAsSeq("errors").head.pathAsString("message")

    def getFirstErrorCode = jsValue.pathAsSeq("errors").head.pathAsLong("code")

    def getFirstFunctionErrorMessage = jsValue.pathAsSeq("errors").head.pathAsString("functionError")
  }

  implicit class PlayJsonAssertionsExtension(json: JsValue) {
    def assertSuccessfulResponse(dataContains: String): Unit = {
      require(
        requirement = !hasErrors,
        message = s"The query had to result in a success but it returned errors. Here's the response: \n $json"
      )

      if (dataContains != "") {
        require(
          requirement = dataContainsString(dataContains),
          message = s"Expected $dataContains to be part of the data object but got: \n $json"
        )
      }
    }

    def assertFailingResponse(errorCode: Int, errorCount: Int, errorContains: String): Unit = {
      require(
        requirement = hasErrors,
        message = s"The query had to result in an error but it returned no errors. Here's the response: \n $json"
      )

      // handle multiple errors, this happens frequently in simple api
      val errors = json.pathAsSeq("errors")
      require(requirement = errors.size == errorCount, message = s"expected exactly $errorCount errors, but got ${errors.size} instead.")

      if (errorCode != 0) {
        val errorCodeInResult = errors.head.pathAsLong("code")
        require(
          requirement = errorCodeInResult == errorCode,
          message = s"Expected the error code $errorCode, but got $errorCodeInResult. Here's the response: \n $json"
        )
      }

      if (errorContains != "") {
        require(
          requirement = errorContainsString(errorContains),
          message = s"Expected $errorContains to be part of the error object but got: \n $json"
        )
      }
    }

    private def hasErrors: Boolean                                = json.asJsObject.value.get("errors").isDefined
    private def dataContainsString(assertData: String): Boolean   = json.asJsObject.value.get("data").toString.contains(assertData)
    private def errorContainsString(assertError: String): Boolean = json.asJsObject.value.get("errors").toString.contains(assertError)

    def assertErrorsAndWarnings(shouldFail: Boolean, shouldWarn: Boolean) = {
      val errors   = json.pathAsSeq("data.deploy.errors")
      val warnings = json.pathAsSeq("data.deploy.warnings")

      (shouldFail, shouldWarn) match {
        case (true, false) =>
          require(requirement = errors.nonEmpty || hasErrors, message = s"The query had to result in a failure but it returned no errors.")
          require(requirement = warnings.isEmpty, message = s"The query had to result in a success but it returned warnings.")

        case (false, false) =>
          require(requirement = errors.isEmpty, message = s"The query had to result in a success but it returned errors.")
          require(requirement = warnings.isEmpty, message = s"The query had to result in a success but it returned warnings.")

        case (false, true) =>
          require(requirement = errors.isEmpty, message = s"The query had to result in a success but it returned errors.")
          require(requirement = warnings.nonEmpty, message = s"The query had to result in a warning but it returned no warnings.")

        case (true, true) =>
          require(requirement = errors.nonEmpty || hasErrors, message = s"The query had to result in a failure but it returned no errors.")
          require(requirement = warnings.nonEmpty, message = s"The query had to result in a warning but it returned no warnings.")
      }
    }
  }

}
