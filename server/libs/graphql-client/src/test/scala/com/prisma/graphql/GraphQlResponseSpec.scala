package cool.graph.graphql

import org.scalatest.{FlatSpec, Matchers}

class GraphQlResponseSpec extends FlatSpec with Matchers {
  val exampleError = errorJson(code = 1111, message = "something did not workout")

  "isSuccess" should "return true if there are NO errors in the response body" in {
    val response = GraphQlResponse(status = 200, body = """ {"data": {"title":"My Todo"} } """)
    response.isSuccess should be(true)
  }

  "isSuccess" should "return false if there are errors in the response body" in {
    val response = GraphQlResponse(status = 200, body = s""" {"data": null, "errors": [$exampleError] } """)
    response.isSuccess should be(false)
  }

  "isFailure" should "return false if there are NO errors in the response body" in {
    val response = GraphQlResponse(status = 200, body = """ {"data": {"title":"My Todo"} } """)
    response.isFailure should be(false)
  }

  "isFailure" should "return true if there are errors in the response body" in {
    val response = GraphQlResponse(status = 200, body = s""" {"data": null, "errors": [$exampleError] } """)
    response.isFailure should be(true)
  }

  "firstError" should "return the first error in a failed response" in {
    val errorCode    = 2222
    val errorMessage = "this is the message of the error"
    val firstError   = errorJson(errorCode, errorMessage)
    val response     = GraphQlResponse(status = 200, body = s""" {"data": null, "errors": [$firstError, $exampleError] } """)

    val error = response.firstError
    error.code should equal(errorCode)
    error.message should equal(errorMessage)
  }

  def errorJson(code: Int, message: String): String = s"""{"code":$code, "message":"$message"}"""
}
