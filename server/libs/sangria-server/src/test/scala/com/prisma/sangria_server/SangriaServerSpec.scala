package com.prisma.sangria_server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http._

trait SangriaServerSpecBase extends WordSpecLike with Matchers with BeforeAndAfterAll {
  import scala.concurrent.ExecutionContext.Implicits.global

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
    response.body should be("""{"message":"hello from the handler"}""")
    response.header("Content-Type") should be(Some("application/json"))
  }

  private def graphQlRequest(query: String): String = {
    s"""
      |{
      |  "query":"$query"
      |}
    """.stripMargin
  }
}

class AkkaHttpSangriaServerSpec extends SangriaServerSpecBase {
  override def executor = AkkaHttpSangriaServer
}
