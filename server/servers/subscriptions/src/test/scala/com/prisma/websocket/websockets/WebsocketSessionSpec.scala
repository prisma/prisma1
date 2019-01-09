package com.prisma.websocket.websockets

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import com.prisma.messagebus.testkits.spechelpers.InMemoryMessageBusTestKits
import com.prisma.subscriptions.TestSubscriptionDependencies
import com.prisma.websocket.WebsocketSession
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
    "send a Poison Pill to the outActor when it is stopped" in {
      val projectId                 = "projectId"
      val sessionId                 = "sessionId"
      val outgoing                  = TestProbe().ref
      val subscriptionsManager      = TestProbe().ref
      val probe                     = TestProbe()
      implicit val testDependencies = new TestSubscriptionDependencies()

      probe.watch(outgoing)

      val session = system.actorOf(Props(WebsocketSession(projectId, sessionId, outgoing, isV7protocol = true, subscriptionsManager)))

      system.stop(session)
      probe.expectTerminated(outgoing)
    }
  }
}
