package cool.graph.subscriptions.specs

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{ScalatestRouteTest, TestFrameworkInterface, WSProbe}
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.ApiTestDatabase
import cool.graph.shared.models.{Project, ProjectId, ProjectWithClientId}
import cool.graph.subscriptions._
import cool.graph.websocket.WebsocketServer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

trait SpecBase extends TestFrameworkInterface with BeforeAndAfterEach with BeforeAndAfterAll with ScalatestRouteTest { this: Suite =>
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val dependencies                 = new SubscriptionDependenciesForTest()
  val testDatabase                          = ApiTestDatabase()
  implicit val actorSytem                   = ActorSystem("test")
  implicit val mat                          = ActorMaterializer()
  val config                                = dependencies.config
  val sssEventsTestKit                      = dependencies.sssEventsTestKit
  val invalidationTestKit                   = dependencies.invalidationTestKit
  val requestsTestKit                       = dependencies.requestsQueueTestKit
  val responsesTestKit                      = dependencies.responsePubSubTestKit

  val wsServer            = WebsocketServer(dependencies)
  val simpleSubServer     = SimpleSubscriptionsServer()
  val subscriptionServers = ServerExecutor(port = 8085, wsServer, simpleSubServer)

  Await.result(subscriptionServers.start, 15.seconds)

  var caseNumber = 1

  override protected def beforeAll(): Unit = {
    super.beforeAll()
//    testDatabase.beforeAllPublic()
  }

  override def beforeEach(): Unit = {
    println((">" * 25) + s" starting test $caseNumber")
    caseNumber += 1
    super.beforeEach()
//    testDatabase.beforeEach()
    sssEventsTestKit.reset
    invalidationTestKit.reset
    responsesTestKit.reset
    requestsTestKit.reset
  }

  override def afterAll(): Unit = {
    println("finished spec " + (">" * 50))
    super.afterAll()
    subscriptionServers.stopBlocking()
//    testDatabase.afterAll()
  }

  def sleep(millis: Long = 2000): Unit = {
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
    import cool.graph.shared.models.ProjectJsonFormatter._
    import cool.graph.stub.Import._

    val projectWithClientId = ProjectWithClientId(project, "clientId")
    val stubs = List(
      cool.graph.stub.Import.Request("GET", s"/${dependencies.projectFetcherPath}/${project.id}").stub(200, Json.toJson(projectWithClientId).toString)
    )
    withStubServer(stubs, port = dependencies.projectFetcherPort) {
      val projectId = ProjectId.fromEncodedString(project.id)
      WS(s"/${projectId.name}/${projectId.stage}", wsClient.flow, Seq(wsServer.v7ProtocolName)) ~> wsServer.routes ~> check {
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
