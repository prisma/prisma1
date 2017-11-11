package cool.graph.akkautil.http

import akka.stream.ActorMaterializer
import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.stub.Import.withStubServer
import cool.graph.stub.StubDsl.Default.Request
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Json

import scala.util.{Failure, Success}

class SimpleHttpClientSpec extends WordSpecLike with Matchers with ScalaFutures {
  implicit val system                  = SingleThreadedActorSystem("server-spec")
  implicit val materializer            = ActorMaterializer()
  override implicit val patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  case class TestResponseClass(field1: String, field2: Seq[String])
  implicit val testResponseClassFormat = Json.format[TestResponseClass]

  case class PostTestClass(data: String)
  implicit val postTestClassFormat = Json.format[PostTestClass]

  val client             = SimpleHttpClient()
  val getStub            = Request("GET", "/some/path").stub(200, """{"field1": "value1", "field2": ["value2.1", "value2.2"]}""").ignoreBody
  val invalidBodyGetStub = Request("GET", "/some/path").stub(200, """{"nope": "invalid"}""").ignoreBody
  val failingGetStub     = Request("GET", "/some/path").stub(500, "Do the panic").ignoreBody
  val getStubInt         = Request("GET", "/some/path").stub(200, "123").ignoreBody
  val getStubWithHeader = Request("GET", "/some/path")
    .stub(200, """{"field1": "value1", "field2": ["value2.1", "value2.2"]}""", Map("x-my-header" -> "someValue"))
    .ignoreBody

  val postStub        = Request("POST", "/some/path", body = """{"data":"testData"}""").stub(200, """{"field1": "value1", "field2": ["value2.1", "value2.2"]}""")
  val failingPostStub = Request("POST", "/some/path").stub(500, "Do the panic").ignoreBody
  val postStubInt     = Request("POST", "/some/path").stub(200, "123").ignoreBody
  val postStubPlain   = Request("POST", "/some/path", body = "1 + 1").stub(200, "2")
  val postStubWithHeader = Request("POST", "/some/path", body = """{"data":"testData"}""")
    .stub(200, """{"field1": "value1", "field2": ["value2.1", "value2.2"]}""", Map("x-my-header" -> "someValue"))
    .ignoreBody

  "The SimpleHttpClient" must {

    "handle GET requests" which {
      "completed with a success status code" in {
        withStubServer(List(getStub)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.body should contain(getStub.stubbedResponse.body)
            response.bodyAs[TestResponseClass] shouldEqual Success(TestResponseClass("value1", Seq("value2.1", "value2.2")))
          }
        }
      }

      "completed with custom headers" in {
        withStubServer(List(getStubWithHeader)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.headers.find(_._1 == "x-my-header") should contain("x-my-header" -> "someValue")
            response.body should contain(getStub.stubbedResponse.body)
            response.bodyAs[TestResponseClass] shouldEqual Success(TestResponseClass("value1", Seq("value2.1", "value2.2")))
          }
        }
      }

      "failed with an unsuccessful status code" in {
        withStubServer(List(failingGetStub)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri).failed) { err: Throwable =>
            err shouldBe an[FailedResponseCodeError]
            err.asInstanceOf[FailedResponseCodeError].response.status shouldEqual 500
          }
        }
      }

      "successfully be able to transform to an int" in {
        withStubServer(List(getStubInt)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.bodyAs[Int] shouldEqual Success(123)
          }
        }
      }

      "that are unable to unmarshal successfully" in {
        withStubServer(List(invalidBodyGetStub)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.bodyAs[TestResponseClass] shouldBe an[Failure[_]]
          }
        }
      }

      "that use a custom status code validator and return a 500 should not fail if the validator passes" in {
        withStubServer(List(failingGetStub)).withArg { server =>
          val uri = s"http://localhost:${server.port}/some/path"

          whenReady(client.get(uri, statusCodeValidator = (_) => true)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 500
          }
        }
      }
    }

    "handle POST requests" which {
      "completed with a success status code" in {
        withStubServer(List(postStub)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = PostTestClass("testData")

          whenReady(client.postJson(uri, body)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.bodyAs[TestResponseClass] shouldEqual Success(TestResponseClass("value1", Seq("value2.1", "value2.2")))
          }
        }
      }

      "completed with custom headers" in {
        withStubServer(List(postStubWithHeader)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = PostTestClass("testData")

          whenReady(client.postJson(uri, body)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.headers.find(_._1 == "x-my-header") should contain("x-my-header" -> "someValue")
            response.body should contain(getStub.stubbedResponse.body)
            response.bodyAs[TestResponseClass] shouldEqual Success(TestResponseClass("value1", Seq("value2.1", "value2.2")))
          }
        }
      }

      "failed with an unsuccessful status code" in {
        withStubServer(List(failingPostStub)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = PostTestClass("testData")

          whenReady(client.postJson(uri, body).failed) { err: Throwable =>
            err shouldBe an[FailedResponseCodeError]
            err.asInstanceOf[FailedResponseCodeError].response.status shouldEqual 500
          }
        }
      }

      "are successfully be able to transform to an int" in {
        withStubServer(List(postStubInt)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = PostTestClass("testData")

          whenReady(client.postJson(uri, body)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.bodyAs[Int] shouldEqual Success(123)
          }
        }
      }

      "are can successfully post plain strings" in {
        withStubServer(List(postStubPlain)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = "1 + 1"

          whenReady(client.post(uri, body)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 200
            response.bodyAs[Int] shouldEqual Success(2)
          }
        }
      }

      "that use a custom status code validator and return a 500 should not fail if the validator passes" in {
        withStubServer(List(failingPostStub)).withArg { server =>
          val uri  = s"http://localhost:${server.port}/some/path"
          val body = PostTestClass("testData")

          whenReady(client.postJson(uri, body, statusCodeValidator = (_) => true)) { (response: SimpleHttpResponse) =>
            response.status shouldEqual 500
          }
        }
      }
    }
  }
}
