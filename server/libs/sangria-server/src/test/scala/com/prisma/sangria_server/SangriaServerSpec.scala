package com.prisma.sangria_server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.{Done, NotUsed}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.http.scaladsl.{Http => AkkaHttp}
import akka.stream.ActorMaterializer
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}
import scalaj.http._

import scala.concurrent.{ExecutionContext, Future}

trait SangriaServerSpecBase extends WordSpecLike with Matchers with BeforeAndAfterAll with AwaitUtils {

  def executor: SangriaServerExecutor

  val handler = new SangriaHandler {
    override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue] = {
      Future.successful(Json.obj("message" -> "hello from the handler"))
    }

    override def supportedWebsocketProtocols = Vector("test-protocol")

    override def newWebsocketSession(request: RawWebsocketRequest): Flow[WebSocketMessage, WebSocketMessage, NotUsed] = {
      Flow[WebSocketMessage].map(msg => WebSocketMessage(s"Received: ${msg.body}"))
    }
  }

  val server = executor.create(handler, port = 8765, requestPrefix = "test")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    server.start()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }

  val serverAddress     = "localhost:8765"
  val httpUrl           = "http://" + serverAddress
  val wsUrl             = "ws://" + serverAddress
  val validGraphQlQuery = "{ someField }"

  "it should return the playground " in {
    val response = Http(httpUrl).asString
    response.body should include("//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/js/middleware.js")
    response.header("Content-Type") should be(Some("text/html; charset=UTF-8"))
  }

  "it should call the handler" in {
    val response = Http(httpUrl).header("content-type", "application/json").postData(graphQlRequest(validGraphQlQuery)).asString
    response.body.asJson should be("""{"message":"hello from the handler"}""".asJson)
    response.header("Content-Type") should be(Some("application/json"))
  }

  "it should reject incoming websocket connections that use the wrong protocol" in {
    val msg                             = "hello world!"
    val (upgradeResponse, firstMessage) = singleWebsocketMessage(msg, protocol = "invalid-protocol")
    upgradeResponse.response.status should not(be(StatusCodes.SwitchingProtocols))
    firstMessage should be(None)
  }

  "it should handle valid websocket connections correctly" in {
    val msg                             = "hello world!"
    val (upgradeResponse, firstMessage) = singleWebsocketMessage(msg, protocol = handler.supportedWebsocketProtocols.head)
    upgradeResponse.response.status should be(StatusCodes.SwitchingProtocols)
    firstMessage.get.asTextMessage.getStrictText should equal(s"Received: $msg")
  }

  private def singleWebsocketMessage(msg: String, protocol: String): (WebSocketUpgradeResponse, Option[Message]) = {
    implicit val system                          = ActorSystem()
    implicit val materializer                    = ActorMaterializer()
    val incoming: Sink[Message, Future[Message]] = Sink.head[Message]
    val outgoing                                 = Source.single(TextMessage(msg))
    val webSocketFlow                            = AkkaHttp().webSocketClientFlow(WebSocketRequest(wsUrl, subprotocol = Some(protocol)))
    val (upgradeResponseFut, firstIncomingMessage) =
      outgoing
        .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
        .toMat(incoming)(Keep.both) // also keep the Future[Message]
        .run()

    val upgradeResponse = upgradeResponseFut.await()

    if (upgradeResponse.response.status == StatusCodes.SwitchingProtocols) {
      (upgradeResponse, Some(firstIncomingMessage.await))
    } else {
      (upgradeResponse, None)
    }
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
