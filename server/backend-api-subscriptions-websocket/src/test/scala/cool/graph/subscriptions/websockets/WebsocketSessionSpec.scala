package cool.graph.subscriptions.websockets

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.messagebus.testkits.RabbitQueueTestKit
import cool.graph.websockets.WebsocketSession
import cool.graph.websockets.protocol.Request
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class WebsocketSessionSpec extends TestKit(ActorSystem("websocket-session-spec")) with WordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures {

  override def afterAll = shutdown()

  val ignoreProbe = TestProbe()
  val ignoreRef   = ignoreProbe.testActor
  val amqpUri     = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI env var required but not found."))

  implicit val bugsnagger = BugSnaggerImpl("")

  "The WebsocketSession" should {
    "send a message with the body STOP to the requests queue AND a Poison Pill to the outActor when it is stopped" in {
      import cool.graph.websockets.protocol.Request._

      val testKit = RabbitQueueTestKit[Request](amqpUri, "subscription-requests", exchangeDurable = true)
      testKit.withTestConsumers()

      val projectId = "projectId"
      val sessionId = "sessionId"
      val outgoing  = TestProbe().ref
      val probe     = TestProbe()

      probe.watch(outgoing)

      val session = system.actorOf(Props(WebsocketSession(projectId, sessionId, outgoing, testKit, bugsnag = null)))

      system.stop(session)
      probe.expectTerminated(outgoing)
      testKit.expectMsg(Request(sessionId, projectId, "STOP"))
    }
  }
}
