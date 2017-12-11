package cool.graph.deploy.specutils

import cool.graph.util.json.PlaySprayConversions
import spray.json._
import play.api.libs.json.{JsValue => PJsValue}

trait GraphQLResponseAssertions extends SprayJsonExtensions {
  import PlaySprayConversions._

  implicit class PlayJsonAssertionsExtension(json: PJsValue) {
    def assertSuccessfulResponse(dataContains: String): Unit = json.toSpray().assertSuccessfulResponse(dataContains)

    def assertFailingResponse(errorCode: Int, errorCount: Int, errorContains: String): Unit =
      json.toSpray().assertFailingResponse(errorCode, errorCount, errorContains)
  }

  implicit class SprayJsonAssertionsExtension(json: JsValue) {
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

    private def hasErrors: Boolean                                = json.asJsObject.fields.get("errors").isDefined
    private def dataContainsString(assertData: String): Boolean   = json.asJsObject.fields.get("data").toString.contains(assertData)
    private def errorContainsString(assertError: String): Boolean = json.asJsObject.fields.get("errors").toString.contains(assertError)

  }
}
