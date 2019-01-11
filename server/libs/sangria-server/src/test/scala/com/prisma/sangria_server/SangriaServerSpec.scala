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
import play.api.libs.json._
import scalaj.http._

import scala.concurrent.{ExecutionContext, Future}

trait SangriaServerSpecBase extends WordSpecLike with Matchers with BeforeAndAfterAll with AwaitUtils {

  def executor: SangriaServerExecutor

  val handler = new SangriaHandler {
    var startHasBeenCalled = 0

    override def onStart() = {
      startHasBeenCalled += 1
      Future.successful(())
    }

    override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue] = {
      Future.successful(Json.obj("message" -> "hello from the handler"))
    }

    override def supportedWebsocketProtocols = Vector("test-protocol")

    override def newWebsocketSession(request: RawWebsocketRequest): Flow[WebSocketMessage, WebSocketMessage, NotUsed] = {
      Flow[WebSocketMessage].map(msg => WebSocketMessage(s"Received: ${msg.body}"))
    }
  }

  val failingHandler = new SangriaHandler {
    override def onStart() = Future.successful(())

    override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext) = sys.error("boom!")
  }

  val requestPrefix = "test"
  val server        = executor.create(handler, port = 8765, requestPrefix = requestPrefix)
  val failingServer = executor.create(failingHandler, port = 8764, requestPrefix = requestPrefix)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    server.start().await()
    failingServer.start().await()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    server.stop().await()
    failingServer.stop().await()
  }

  val serverAddress     = "localhost:8765"
  val httpUrl           = "http://" + serverAddress
  val failingHttpUrl    = "http://localhost:8764"
  val wsUrl             = "ws://" + serverAddress
  val validGraphQlQuery = "{ someField }"

  "it should return an error if the query is not valid" in {
    val invalidQuery = "woot"
    val response     = Http(httpUrl).header("content-type", "application/json").postData(graphQlRequest(invalidQuery)).asString
    response.code should be(500)
    response.header("Content-Type") should be(Some("application/json"))
    val errors = response.body.asJsObject.value("errors").asInstanceOf[JsArray]
    errors.value should have(size(1))
    errors.head.as[JsObject].value("message").as[JsString].value should include("Syntax error while parsing GraphQL query. Invalid input")
  }

  "start on the handler must have been called" in {
    import scala.language.reflectiveCalls
    handler.startHasBeenCalled should be(1)
  }

  "it should return the playground " in {
    val response = Http(httpUrl).asString
    response.body should include("//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/js/middleware.js")
    response.header("Content-Type") should (
      be(Some("text/html; charset=UTF-8")) or
        be(Some("text/html"))
    )
  }

  "it should call the handler" in {
    val response = Http(httpUrl).header("content-type", "application/json").postData(graphQlRequest(validGraphQlQuery)).asString
    response.code should be(200)
    response.body.asJson should be("""{"message":"hello from the handler"}""".asJson)
    response.header("Content-Type") should be(Some("application/json"))
    val requestIdHeader = response.header("Request-Id")
    requestIdHeader should not(be(empty))
    requestIdHeader.get should startWith(requestPrefix + ":")
  }

  "it should support batching" in {
    val singleQuery    = Json.obj("query" -> validGraphQlQuery)
    val batchedRequest = JsArray((1 to 3).map(_ => singleQuery))
    val response       = Http(httpUrl).header("content-type", "application/json").postData(batchedRequest.toString).asString
    response.code should be(200)
    val singleReponse = """{"message":"hello from the handler"}""".asJson
    response.body.asJson should be(JsArray((1 to 3).map(_ => singleReponse)))
    response.header("Content-Type") should be(Some("application/json"))
  }

  "it should have a status endpoint" in {
    val response = Http(httpUrl + "/status").asString
    response.body should equal("\"OK\"")
  }

  "it should return a properly formatted error if an exception occurs" in {
    val response = Http(failingHttpUrl).header("content-type", "application/json").postData(graphQlRequest(validGraphQlQuery)).asString
    response.code should be(500)
    response.header("Content-Type") should be(Some("application/json"))
    val requestIdHeader = response.header("Request-Id")
    requestIdHeader should not(be(empty))
    requestIdHeader.get should startWith(requestPrefix + ":")
    val errors = response.body.asJsObject.value("errors").asInstanceOf[JsArray]
    errors.value should have(size(1))
    errors.head.as[JsObject].value("message") should equal(JsString("boom!"))
  }

  "it should reject incoming websocket connections that use the wrong protocol" in {
    val msg                             = "hello world!"
    val (upgradeResponse, firstMessage) = singleWebsocketMessage(msg, protocol = "invalid-protocol")
    upgradeResponse.response.status should not(be(StatusCodes.SwitchingProtocols))
    firstMessage should be(None)
  }

  "it should handle valid websocket connections correctly" in {
    if (executor.supportsWebsockets) {
      val msg                             = "hello world!"
      val (upgradeResponse, firstMessage) = singleWebsocketMessage(msg, protocol = handler.supportedWebsocketProtocols.head)
      upgradeResponse.response.status should be(StatusCodes.SwitchingProtocols)
      firstMessage.get.asTextMessage.getStrictText should equal(s"Received: $msg")
    }
  }

  def singleWebsocketMessage(msg: String, protocol: String): (WebSocketUpgradeResponse, Option[Message]) = {
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

  def graphQlRequest(query: String): String = {
    s"""
      |{
      |  "query":"$query"
      |}
    """.stripMargin
  }

  implicit class StringExtensions(str: String) {
    def asJson: JsValue      = Json.parse(str)
    def asJsObject: JsObject = asJson.asInstanceOf[JsObject]
  }
}

class AkkaHttpSangriaServerSpec extends SangriaServerSpecBase {
  override def executor = AkkaHttpSangriaServer
}

class BlazeSangriaServerSpec extends SangriaServerSpecBase {
  override def executor = BlazeSangriaServer
}
