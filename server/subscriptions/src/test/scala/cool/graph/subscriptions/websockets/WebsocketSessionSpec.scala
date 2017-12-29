package cool.graph.subscriptions.websockets

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import cool.graph.messagebus.testkits.spechelpers.InMemoryMessageBusTestKits
import cool.graph.subscriptions.WebsocketSession
import cool.graph.subscriptions.protocol.Request
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class WebsocketSessionSpec
    extends InMemoryMessageBusTestKits(ActorSystem("websocket-session-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  override def afterAll = shutdown()

  "The WebsocketSession" should {
    "send a message with the body STOP to the requests queue AND a Poison Pill to the outActor when it is stopped" in {
      withQueueTestKit[Request] { testKit =>
        val projectId = "projectId"
        val sessionId = "sessionId"
        val outgoing  = TestProbe().ref
        val probe     = TestProbe()

        probe.watch(outgoing)

        val session = system.actorOf(Props(WebsocketSession(projectId, sessionId, outgoing, testKit, bugsnag = null)))

        system.stop(session)
        probe.expectTerminated(outgoing)
        testKit.expectPublishedMsg(Request(sessionId, projectId, "STOP"))
      }
    }
  }
}
