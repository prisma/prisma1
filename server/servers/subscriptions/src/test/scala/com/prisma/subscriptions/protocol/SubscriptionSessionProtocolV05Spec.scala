package com.prisma.subscriptions.protocol

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import com.prisma.shared.schema_dsl.TestProject
import com.prisma.subscriptions.TestSubscriptionDependencies
import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.{CreateSubscription, EndSubscription}
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.CreateSubscriptionSucceeded
import org.scalatest._
import play.api.libs.json.Json

import scala.concurrent.duration._

class SubscriptionSessionProtocolV05Spec extends TestKit(ActorSystem("subscription-manager-spec")) with WordSpecLike with Matchers with BeforeAndAfterAll {

  import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Requests._
  import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses._

  override def afterAll: Unit = shutdown()

  implicit val materializer  = ActorMaterializer()
  implicit val dependencies  = new TestSubscriptionDependencies
  val ignoreProbe: TestProbe = TestProbe()
  val ignoreRef: ActorRef    = ignoreProbe.testActor

  def ignoreKeepAliveProbe: TestProbe = {
    val ret = TestProbe()
    ret.ignoreMsg {
      case SubscriptionKeepAlive => true
    }
    ret
  }

  val projectId = "projectId"

  "Sending an INIT message" should {
    "succeed when the payload is empty" in withProjectFetcherStub(projectId) {
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! InitConnection(Some(emptyPayload))
      parent.expectMsg(InitConnectionSuccess)
    }

    "succeed when the payload contains a String in the Authorization field" in withProjectFetcherStub(projectId) {
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val payloadWithAuth     = Json.obj("Authorization" -> "abc")

      subscriptionSession ! InitConnection(Some(payloadWithAuth))
      parent.expectMsg(InitConnectionSuccess)
    }
  }

  "Sending SUBSCRIPTION_START after an INIT" should {
    "respond with SUBSCRIPTION_FAIL when the query is not valid GraphQL" in withProjectFetcherStub(projectId) {
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! InitConnection(Some(emptyPayload))
      parent.expectMsg(InitConnectionSuccess)

      // actual test
      val invalidQuery = // no projection so it is invalid
        """
          | query {
          |   whatever(id: "bla"){}
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(Some("subscription-id"), None)
      val start          = SubscriptionStart(subscriptionId, invalidQuery, variables = None, operationName = None)

      subscriptionSession ! start

      val lastResponse = parent.expectMsgType[SubscriptionFail]
      lastResponse.id should be(subscriptionId)
      lastResponse.payload.errors.head.message should include("Query was not valid")
    }

    "respond with SUBSCRIPTION_SUCCESS if " +
      "1. the query is valid " +
      "2. the subscriptions manager received CreateSubscription " +
      "3. and the manager responded with CreateSubscriptionSucceeded" in withProjectFetcherStub(projectId) {
      val testProbe           = TestProbe()
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(testProbe.ref)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! InitConnection(Some(emptyPayload))
      parent.expectMsg(InitConnectionSuccess)

      // actual test
      val validQuery =
        """
          | query {
          |   whatever(id: "bla"){
          |     id
          |   }
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(Some("subscription-id"), None)
      val start          = SubscriptionStart(subscriptionId, validQuery, variables = None, operationName = None)

      subscriptionSession ! start

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      parent.expectMsg(SubscriptionSuccess(subscriptionId))
    }
  }

  "Sending SUBSCRIPTION_END after a SUBSCRIPTION_START" should {
    "result in an EndSubscription message being sent to the subscriptions manager IF a subscription id is supplied" in {
      val testProbe           = TestProbe()
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(testProbe.ref)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! InitConnection(Some(emptyPayload))
      parent.expectMsg(InitConnectionSuccess)

      val validQuery =
        """
          | query {
          |   whatever(id: "bla"){
          |     id
          |   }
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(Some("subscription-id"), None)
      val start          = SubscriptionStart(subscriptionId, validQuery, variables = None, operationName = None)
      subscriptionSession ! start

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      parent.expectMsg(SubscriptionSuccess(subscriptionId))

      // actual test
      subscriptionSession ! SubscriptionEnd(Some(subscriptionId))

      val endMsg = testProbe.expectMsgType[EndSubscription]

      endMsg.id should equal(subscriptionId)
      endMsg.projectId should equal("projectId")
    }

    "result in no message being sent to the subscriptions manager IF NO subscription id is supplied" in {
      val testProbe           = TestProbe()
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(testProbe.ref)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! InitConnection(Some(emptyPayload))
      parent.expectMsg(InitConnectionSuccess)

      val validQuery =
        """
          | query {
          |   whatever(id: "bla"){
          |     id
          |   }
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(Some("subscription-id"), None)
      val start          = SubscriptionStart(subscriptionId, validQuery, variables = None, operationName = None)

      subscriptionSession ! start

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      parent.expectMsg(SubscriptionSuccess(subscriptionId))

      // actual test
      subscriptionSession ! SubscriptionEnd(None)
      testProbe.expectNoMessage(3.seconds)
    }
  }

  def withProjectFetcherStub[T](projectId: String)(fn: => T) = {
    val project = TestProject.emptyV11.copy(id = projectId)
    dependencies.projectFetcher.put(projectId, project)
    fn
  }

  def subscriptionSessionActor(subscriptionsManager: ActorRef) = new SubscriptionSessionActorV05("sessionId", "projectId", subscriptionsManager)
}
