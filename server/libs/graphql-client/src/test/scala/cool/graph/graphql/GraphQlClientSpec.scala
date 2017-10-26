package cool.graph.graphql

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Awaitable}

class GraphQlClientSpec extends FlatSpec with Matchers {
  import cool.graph.stub.Import._
  import scala.concurrent.ExecutionContext.Implicits.global

  val stub = Request("POST", "/graphql-endpoint").stub(200, """{"data": {"id": "1234"}}""").ignoreBody

  "sendQuery" should "send the correct the correct JSON structure to the server" in {
    withStubServer(List(stub)).withArg { server =>
      val uri    = s"http://localhost:${server.port}${stub.path}"
      val client = GraphQlClient(uri)
      val query  = """ { mutation { createTodo(title:"the title"){id} }} """
      val result = await(client.sendQuery(query))

      val expectedBody = s"""{"query":"${escapeQuery(query)}"}"""
      server.lastRequest.body should equal(expectedBody)

      result.status should equal(stub.stubbedResponse.status)
      result.body should equal(stub.stubbedResponse.body)
    }
  }

  "sendQuery" should "send the specified headers to the server" in {
    withStubServer(List(stub)).withArg { server =>
      val uri     = s"http://localhost:${server.port}${stub.path}"
      val header1 = "Header1" -> "Header1Value"
      val header2 = "Header2" -> "Header2Value"
      val headers = Map(header1, header2)
      val client  = GraphQlClient(uri, headers)
      val query   = """ { mutation { createTodo(title:"the title"){id} }} """
      val result  = await(client.sendQuery(query))

      server.lastRequest.headers should contain(header1)
      server.lastRequest.headers should contain(header2)

      result.status should equal(stub.stubbedResponse.status)
      result.body should equal(stub.stubbedResponse.body)
    }
  }

  def escapeQuery(query: String) = query.replace("\"", "\\\"")

  def await[T](awaitable: Awaitable[T]): T = {
    import scala.concurrent.duration._
    Await.result(awaitable, 5.seconds)
  }
}
