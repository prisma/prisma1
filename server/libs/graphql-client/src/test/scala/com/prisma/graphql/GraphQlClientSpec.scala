package com.prisma.graphql

import com.prisma.stub.Stub
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Awaitable}

class GraphQlClientSpec extends FlatSpec with Matchers {
  import com.prisma.stub.Import._

  val defaultStub = stub("/graphql-endpoint")

  "sendQuery" should "send the correct the correct JSON structure to the server" in {
    withStubs(defaultStub).withArg { server =>
      val uri    = s"http://localhost:${server.port}${defaultStub.path}"
      val client = GraphQlClient(uri)
      val query  = """ { mutation { createTodo(title:"the title"){id} }} """
      val result = await(client.sendQuery(query))

      val expectedBody = s"""{"query":"${escapeQuery(query)}","variables":{}}"""
      server.lastRequest.body should equal(expectedBody)

      result.status should equal(defaultStub.stubbedResponse.status)
      result.body should equal(defaultStub.stubbedResponse.body)
    }
  }

  "sendQuery" should "send the specified headers to the server" in {
    withStubs(defaultStub).withArg { server =>
      val uri     = s"http://localhost:${server.port}${defaultStub.path}"
      val header1 = "Header1" -> "Header1Value"
      val header2 = "Header2" -> "Header2Value"
      val headers = Map(header1, header2)
      val client  = GraphQlClient(uri, headers)
      val query   = """ { mutation { createTodo(title:"the title"){id} }} """
      val result  = await(client.sendQuery(query))

      server.lastRequest.headers should contain(header1)
      server.lastRequest.headers should contain(header2)

      result.status should equal(defaultStub.stubbedResponse.status)
      result.body should equal(defaultStub.stubbedResponse.body)
    }
  }

  "sendQuery" should "use the specified path and headers arguments" in {
    val path = "/mypath"
    withStubs(stub(path)).withArg { server =>
      val uri          = s"http://localhost:${server.port}"
      val header1      = "Header1" -> "Header1Value"
      val header2      = "Header2" -> "Header2Value"
      val baseHeaders  = Map(header1)
      val extraHeaders = Map(header2)
      val client       = GraphQlClient(uri, baseHeaders)
      await(client.sendQuery(query = "irrelevant", path = path, headers = extraHeaders))
      server.lastRequest.path should equal(path)
      server.lastRequest.headers should contain(header1)
      server.lastRequest.headers should contain(header2)
    }
  }

  def withStubs(stubs: Stub*) = withStubServer(List(stubs: _*), stubNotFoundStatusCode = 418)

  def stub(path: String): Stub = Request("POST", path).stub(200, """{"data": {"id": "1234"}}""").ignoreBody

  def escapeQuery(query: String) = query.replace("\"", "\\\"")

  def await[T](awaitable: Awaitable[T]): T = {
    import scala.concurrent.duration._
    Await.result(awaitable, 5.seconds)
  }
}
