package cool.graph.subscriptions.specs

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{ScalatestRouteTest, TestFrameworkInterface, WSProbe}
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.ApiTestDatabase
import cool.graph.bugsnag.{BugSnaggerImpl, BugSnaggerMock}
import cool.graph.shared.models.{Project, ProjectWithClientId}
import cool.graph.subscriptions._
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.websocket.WebsocketServer
import cool.graph.websocket.protocol.Request
import cool.graph.websocket.services.WebsocketDevDependencies
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.Await
import scala.concurrent.duration._

trait SpecBase extends TestFrameworkInterface with BeforeAndAfterEach with BeforeAndAfterAll with ScalatestRouteTest { this: Suite =>
  implicit val bugsnag      = BugSnaggerMock
  implicit val ec           = system.dispatcher
  implicit val dependencies = new SubscriptionDependenciesForTest()
  val testDatabase          = ApiTestDatabase()
  implicit val actorSytem   = ActorSystem("test")
  implicit val mat          = ActorMaterializer()
  val config                = dependencies.config
  val sssEventsTestKit      = dependencies.sssEventsTestKit
  val invalidationTestKit   = dependencies.invalidationTestKit
  val requestsTestKit       = dependencies.requestsQueueTestKit
  val responsesTestKit      = dependencies.responsePubSubTestKit

  val websocketServices = WebsocketDevDependencies(
    requestsQueuePublisher = requestsTestKit.map[Request] { req: Request =>
      SubscriptionRequest(req.sessionId, req.projectId, req.body)
    },
    responsePubSubSubscriber = responsesTestKit
  )

  val wsServer            = WebsocketServer(websocketServices)
  val simpleSubServer     = SimpleSubscriptionsServer()
  val subscriptionServers = ServerExecutor(port = 8085, wsServer, simpleSubServer)

  Await.result(subscriptionServers.start, 15.seconds)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
//    testDatabase.beforeAllPublic()
  }

  override def beforeEach() = {
    super.beforeEach()

//    testDatabase.beforeEach()
    sssEventsTestKit.reset
    invalidationTestKit.reset
    responsesTestKit.reset
    requestsTestKit.reset
  }

  override def afterAll() = {
    println("finished spec " + (">" * 50))
    super.afterAll()
//    testDatabase.afterAll()
    subscriptionServers.stopBlocking()
  }

  def sleep(millis: Long = 2000) = {
    Thread.sleep(millis)
  }

  def testInitializedWebsocket(project: Project)(checkFn: WSProbe => Unit): Unit = {
    testWebsocket(project) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(connectionAck)
      checkFn(wsClient)
    }
  }

  def testWebsocket(project: Project)(checkFn: WSProbe => Unit): Unit = {
    val wsClient = WSProbe()
    import cool.graph.stub.Import._
    import cool.graph.shared.models.ProjectJsonFormatter._

    val projectWithClientId = ProjectWithClientId(project, "clientId")
    val stubs = List(
      cool.graph.stub.Import.Request("GET", s"/system/${project.id}").stub(200, Json.toJson(projectWithClientId).toString)
    )
    withStubServer(stubs, port = 9000) {
      WS(s"/v1/${project.id}", wsClient.flow, Seq(wsServer.subProtocol2)) ~> wsServer.routes ~> check {
        checkFn(wsClient)
      }
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

}
