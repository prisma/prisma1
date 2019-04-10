package com.prisma.subscriptions.specs

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{ScalatestRouteTest, TestFrameworkInterface, WSProbe}
import akka.stream.ActorMaterializer
import com.prisma.ConnectorAwareTest
import com.prisma.api.ApiTestDatabase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, RelationLinkListCapability}
import com.prisma.shared.models.{ConnectorCapability, Project}
import com.prisma.subscriptions._
import com.prisma.utils.await.AwaitUtils
import com.prisma.websocket.WebSocketHandler
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContextExecutor

trait SubscriptionSpecBase
    extends ConnectorAwareTest
    with AwaitUtils
    with TestFrameworkInterface
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalatestRouteTest {
  this: Suite =>

  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val dependencies                 = new TestSubscriptionDependencies()
  implicit lazy val implicitSuite           = this
  implicit lazy val deployConnector         = dependencies.deployConnector
  val testDatabase                          = ApiTestDatabase()
  implicit val actorSytem                   = ActorSystem("test")
  implicit val mat                          = ActorMaterializer()
  val sssEventsTestKit                      = dependencies.sssEventsTestKit
  val invalidationTestKit                   = dependencies.invalidationTestKit
  val projectIdEncoder                      = dependencies.projectIdEncoder

  override def capabilities = dependencies.apiConnector.capabilities

  override def prismaConfig = dependencies.config

  val wsServer = WebSocketHandler(dependencies)

  var caseNumber = 1

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    deployConnector.initialize().await()
  }

  override def beforeEach(): Unit = {
    println((">" * 25) + s" starting test $caseNumber")
    caseNumber += 1
    super.beforeEach()
    sssEventsTestKit.reset
    invalidationTestKit.reset
  }

  override def afterAll(): Unit = {
    println("finished spec " + (">" * 50))
    super.afterAll()
  }

  def sleep(millis: Long = 2000): Unit = {
    Thread.sleep(millis)
  }

  def testInitializedWebsocket(project: Project)(checkFn: WSProbe => Unit): Unit = {
    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(connectionAck)
      checkFn(wsClient)
    }
  }

  def testWebsocketV07(project: Project)(checkFn: WSProbe => Unit): Unit = testWebsocket(project, wsServer.v7ProtocolName)(checkFn)
  def testWebsocketV05(project: Project)(checkFn: WSProbe => Unit): Unit = testWebsocket(project, wsServer.v5ProtocolName)(checkFn)

  private def testWebsocket(project: Project, wsSubProtocol: String)(checkFn: WSProbe => Unit): Unit = {
    val wsClient              = WSProbe()
    val dummyStage            = "test"
    val projectIdInFetcherUrl = projectIdEncoder.toEncodedString(project.id, dummyStage)
    dependencies.projectFetcher.put(projectIdInFetcherUrl, project)

    WS(s"/${project.id}/$dummyStage", wsClient.flow, Seq(wsSubProtocol)) ~> wsServer.routes ~> check {
      checkFn(wsClient)
    }
  }

  /**
    * MESSAGES FOR PROTOCOL VERSION 0.7
    */
  val cantBeParsedError      = """{"id":"","payload":{"message":"The message can't be parsed"},"type":"error"}"""
  val connectionAck          = """{"type":"connection_ack"}"""
  val connectionInit: String = connectionInit(None)

  def connectionInit(token: String): String = connectionInit(Some(token))

  def connectionInit(token: Option[String]): String = token match {
    case Some(token) => s"""{"type":"connection_init","payload":{"Authorization": "Bearer $token"}}"""
    case None        => s"""{"type":"connection_init","payload":{}}"""
  }

  def startMessage(id: String, query: String, variables: JsObject = Json.obj()): String = {
    startMessage(id, query, variables = variables, operationName = None)
  }

  def startMessage(id: String, query: String, operationName: String): String = {
    startMessage(id, query, Json.obj(), Some(operationName))
  }

  def startMessage(id: String, query: String, variables: JsValue, operationName: Option[String]): String = {
    Json
      .obj(
        "id"   -> id,
        "type" -> "start",
        "payload" -> Json.obj(
          "variables"     -> variables,
          "operationName" -> operationName,
          "query"         -> query
        )
      )
      .toString
  }

  def startMessage(id: Int, query: String, variables: JsValue, operationName: Option[String]): String = {
    Json
      .obj(
        "id"   -> id,
        "type" -> "start",
        "payload" -> Json.obj(
          "variables"     -> variables,
          "operationName" -> operationName,
          "query"         -> query
        )
      )
      .toString
  }

  def stopMessage(id: String): String = s"""{"type":"stop","id":"$id"}"""
  def stopMessage(id: Int): String    = s"""{"type":"stop","id":"$id"}"""

  def dataMessage(id: String, payload: String): String = {
    val payloadAsJson = Json.parse(payload)
    Json
      .obj(
        "id" -> id,
        "payload" -> Json.obj(
          "data" -> payloadAsJson
        ),
        "type" -> "data"
      )
      .toString
  }

  def dataMessage(id: Int, payload: String): String = {
    val payloadAsJson = Json.parse(payload)
    Json
      .obj(
        "id" -> id,
        "payload" -> Json.obj(
          "data" -> payloadAsJson
        ),
        "type" -> "data"
      )
      .toString
  }

  def errorMessage(id: String, message: String): String = {
    Json
      .obj(
        "id" -> id,
        "payload" -> Json.obj(
          "message" -> message
        ),
        "type" -> "error"
      )
      .toString
  }

  val listInlineDirective = if (capabilities.has(RelationLinkListCapability)) {
    "@relation(link: INLINE)"
  } else {
    ""
  }

  val listInlineArgument = if (capabilities.has(RelationLinkListCapability)) {
    "link: INLINE"
  } else {
    ""
  }

  val scalarListDirective = if (capabilities.hasNot(EmbeddedScalarListsCapability)) {
    "@scalarList(strategy: RELATION)"
  } else {
    ""
  }
}
