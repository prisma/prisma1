package cool.graph.stub

import javax.servlet.http.HttpServletRequest

import org.specs2.mutable.Specification

import scala.collection.SortedMap
import scala.util.Random
import scalaj.http.{Http, HttpResponse}

class StubServerSpec extends Specification {
  import cool.graph.stub.Import._

  def mustBeNotFoundResponse(response: HttpResponse[String]) = {
    response.code must equalTo(999)
    response.body must contain(""" "message": "No stub found for request [URL: """)
  }
  def mustBeNoStubsResponse(response: HttpResponse[String]) = {
    response.code must equalTo(999)
    response.body must contain(""" "There are no registered stubs in the server!" """)
  }

  val em = SortedMap.empty[String, String]
  import cool.graph.stub.StubDsl.Default.Request

  "StubServer" should {
    "respond with the default 'not found' stub if there are no stubs" in {
      withStubServer(List.empty).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("q", "monkeys").asString
        mustBeNoStubsResponse(response)
      }
    }

    "respond with the desired stubNotFoundStatusCode" in {
      withStubServer(List.empty, stubNotFoundStatusCode = 101).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("q", "monkeys").asString
        response.code mustEqual 101
      }
    }

    "respond with the default 'not found' stub if there are stubs, but no one matches the request" in {
      val stubs = List(Request("GET", "/path").stub(200, "whatever"))
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/another-path").method("GET").asString
        mustBeNotFoundResponse(response)
      }
    }

    "respond with the stub that is matching the request by Method and Path" in {
      val stubs = List(Request("GET", "/path").stub(200, "response"))
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path").asString
        response.code mustEqual 200
        response.body mustEqual "response"
        val response2: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path?param=value").asString
        response.body mustEqual response2.body
      }
    }

    "respond with the stub that matches HTTP method AND Path, when multiple stubs differ in the method" in {
      val stubs = List(
        Request("GET", "/path").stub(StaticStubResponse(200, "get-response")),
        Request("POST", "/path").stub(StaticStubResponse(200, "post-response"))
      )
      withStubServer(stubs).withArg { server =>
        val getResponse: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path").method("GET").asString
        getResponse.code must equalTo(200)
        getResponse.body mustEqual "get-response"
        val postResponse: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path").method("POST").asString
        postResponse.code must equalTo(200)
        postResponse.body mustEqual "post-response"
      }
    }

    "respond with the stub that only matches HTTP method AND Path even though the request includes query params" in {
      val successfulExistenz = Request("GET", "/path").stub(200, "path-response")
      val anotherStub        = Request("GET", "/another-path").stub(200, "another-path-response")
      withStubServer(List(successfulExistenz, anotherStub)).withArg { server =>
        val response: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path")
          .param("q", "monkeys")
          .param("list_id", "123")
          .method("GET")
          .asString
        response.code mustEqual 200
        response.body mustEqual "path-response"
      }
    }

    "respond with the stub that matches more Query Params if Method AND Path match" in {
      val aufgabenStub             = Request("GET", "/path", Map("list_id" -> "123")).stub(200, "more-matching")
      val lessMatchingAufgabenStub = Request("GET", "/path").stub(200, "less-matching")

      val stubs = List(lessMatchingAufgabenStub, aufgabenStub)
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("list_id", "123").asString
        response.code mustEqual 200
        response.body mustEqual "more-matching"
      }
    }

    "respond with the correct stub if the request has multiple query params" in {
      val aufgabenStub =
        Request("GET", "/path", Map("list_id" -> "123", "user_id" -> "123")).stub(200, "more-matching")
      val tasksStub = Request("GET", "/path", Map("list_id" -> "123")).stub(200, "less-matching")

      val stubs = List(tasksStub, aufgabenStub)
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("list_id", "123").param("user_id", "123").asString
        response.code must equalTo(200)
        response.body mustEqual "more-matching"
      }
    }

    "respond with the correct stub if the request query params and the stub query params are NOT in the same order" in {
      val aufgabenStub =
        Request("GET", "/path", Map("list_id" -> "123", "user_id" -> "123")).stub(200, "more-matching")
      val tasksStub = Request("GET", "/path").stub(200, "less-matching")

      val stubs = List(tasksStub, aufgabenStub)
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("user_id", "123").param("list_id", "123").asString
        response.code mustEqual 200
        response.body mustEqual "more-matching"
      }
    }
    "respond with the stub that matches HTTP method AND Path and a PART of the QueryString" in {
      val lessMatchingAufgabenStub = Request("GET", "/path").stub(200, "less-matching")
      val aufgabenStub             = Request("GET", "/path", Map("list_id" -> "123")).stub(200, "more-matching")

      val stubs = List(lessMatchingAufgabenStub, aufgabenStub)
      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").param("list_id", "123").param("foo", "bar").asString
        println(response.body)
        response.code mustEqual 200
        response.body mustEqual "more-matching"
      }
    }

    "respond with no Stub, if the stub matches Path, Method and QueryString BUT NOT Body " in {
      val body = """{ "foo" : "bar" }"""
      val stub = Request("POST", "/path", Map("a" -> "b"), body).stub(200, "response")
      withStubServer(List(stub)).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").postData("Not the same body").param("a", "b").asString
        println(response.body)
        response.code mustEqual 999
      }
    }

    "respond with a Stub, if the stub matches Path, Method and QueryString BUT NOT Body, but ignoreBody is used" in {
      val body = """{ "foo" : "bar" }"""
      val stub = Request("POST", "/path", Map("a" -> "b"), body).stub(200, "response").ignoreBody
      withStubServer(List(stub)).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").postData("Not the same body").param("a", "b").asString
        println(response.body)
        response.code mustEqual 200
        response.body mustEqual "response"
      }
    }

    "respond with Stub, if the stub matches Path, Method and QueryString and Body " in {
      val body = """{ "foo" : "bar", "x" : "y" }"""
      val bodyInDifferentOrder =
        """{
          |"x" : "y",
          |"foo" : "bar"
          |}""".stripMargin
      val stub = Request("POST", "/path", Map("a" -> "b"), body).stub(200, "response")
      withStubServer(List(stub)).withArg { server =>
        val response: HttpResponse[String] =
          Http(s"http://127.0.0.1:${server.port}/path").postData(bodyInDifferentOrder).param("a", "b").asString
        println(response.body)
        response.code mustEqual 200
      }
    }

    "with requestCount one can check the number of calls to a particular stub" in {
      val stub1 = Request("GET", "/path").stub(200, "some-response")
      val stub2 = Request("POST", "/path").stub(200, "another-response")

      withStubServer(List(stub1, stub2)).withArg { server =>
        server.requestCount(stub1) mustEqual 0
        server.requestCount(stub2) mustEqual 0

        Http(s"http://127.0.0.1:${server.port}/path").asString
        server.requestCount(stub1) mustEqual 1
        Http(s"http://127.0.0.1:${server.port}/path").asString
        server.requestCount(stub1) mustEqual 2
        server.requestCount(stub2) mustEqual 0
      }
    }

    "respond with a given header in the stub response" in {
      val stubs = List(Request("GET", "/path").stub(200, "response", Map("X-Test-Header" -> "value")))

      withStubServer(stubs).withArg { server =>
        val response: HttpResponse[String] = Http(s"http://127.0.0.1:${server.port}/path").asString
        response.code mustEqual 200
        response.body mustEqual "response"
        response.headers.get("X-Test-Header").get must equalTo("value")
      }
    }

    "withStubServer should compile just fine for the apply without arguments" in {
      val stub1 = Request("GET", "/path").stub(200, "some-response")

      withStubServer(List(stub1)) {
        true must beTrue
      }
    }

    class FailingStubHandler extends StubServerHandler(List.empty, 999) {
      override def stubResponseForRequest(request: StubRequest): StaticStubResponse =
        throw new Exception("this goes boom!")
    }
    class FailingServer extends StubServer(stubs = List.empty, port = 8000 + Random.nextInt(1000), stubNotFoundStatusCode = 999) {
      override def createHandler: StubServerHandler = new FailingStubHandler
    }

    def withFailingServer[T](block: FailingServer => T): T = {
      val server = new FailingServer
      try {
        server.start
        block(server)
      } finally {
        server.stop
      }
    }
    "the StubHandler should respond with a proper description if the stub matching fails" in {
      withFailingServer { server =>
        val response = Http(s"http://127.0.0.1:${server.port}/path").asString
        response.body must contain("Stub Matching failed")
        response.code mustEqual 999
      }
    }
  }
}
