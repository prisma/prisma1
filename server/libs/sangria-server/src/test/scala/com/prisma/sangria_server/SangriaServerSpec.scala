package com.prisma.sangria_server

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}
import scalaj.http._

import scala.concurrent.{ExecutionContext, Future}

trait SangriaServerSpecBase extends WordSpecLike with Matchers with BeforeAndAfterAll {

  def executor: SangriaServerExecutor

  val handler = new RequestHandler {
    override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue] = {
      Future.successful(Json.obj("message" -> "hello from the handler"))
    }
  }

  val server = executor.create(handler, port = 8765)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    server.start()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }

  val serverUrl         = "http://localhost:8765"
  val validGraphQlQuery = "{ someField }"

  "it should return the playground " in {
    val response = Http(serverUrl).asString
    response.body should include("//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/js/middleware.js")
    response.header("Content-Type") should be(Some("text/html; charset=UTF-8"))
  }

  "it should call the handler" in {
    val response = Http(serverUrl).header("content-type", "application/json").postData(graphQlRequest(validGraphQlQuery)).asString
    response.body.asJson should be("""{"message":"hello from the handler"}""".asJson)
    response.header("Content-Type") should be(Some("application/json"))
  }

  private def graphQlRequest(query: String): String = {
    s"""
      |{
      |  "query":"$query"
      |}
    """.stripMargin
  }

  implicit class StringExtensions(str: String) {
    def asJson: JsValue = Json.parse(str)
  }
}

class AkkaHttpSangriaServerSpec extends SangriaServerSpecBase {
  override def executor = AkkaHttpSangriaServer
}
