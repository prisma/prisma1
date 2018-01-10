package cool.graph.subscriptions.protocol

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import cool.graph.bugsnag.{BugSnagger, BugSnaggerMock}
import cool.graph.messagebus.pubsub.Message
import cool.graph.messagebus.testkits._
import cool.graph.subscriptions.SubscriptionDependenciesForTest
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionSessionManager.Requests.EnrichedSubscriptionRequestV05
import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.{CreateSubscription, EndSubscription}
import cool.graph.subscriptions.resolving.SubscriptionsManager.Responses.CreateSubscriptionSucceeded
import org.scalatest._
import play.api.libs.json.Json

import scala.concurrent.duration._

class SubscriptionSessionManagerProtocolV05Spec
    extends TestKit(ActorSystem("subscription-manager-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Requests._
  import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses._

  implicit val materializer = ActorMaterializer()

  override def afterAll: Unit = shutdown()

  val ignoreProbe: TestProbe = TestProbe()
  val ignoreRef: ActorRef    = ignoreProbe.testActor
  val bugsnag: BugSnagger    = BugSnaggerMock
  implicit val dependencies  = new SubscriptionDependenciesForTest

  def ignoreKeepAliveProbe: TestProbe = {
    val ret = TestProbe()
    ret.ignoreMsg {
      case SubscriptionKeepAlive => true
    }
    ret
  }

  "Sending an INIT message" should {
    "succeed when the payload is empty" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val manager      = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef, bugsnag)))
      val emptyPayload = Json.obj()

      manager ! EnrichedSubscriptionRequestV05("sessionId", "projectId", InitConnection(Some(emptyPayload)))
      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)
    }

    "succeed when the payload contains a String in the Authorization field" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val manager         = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef, bugsnag)))
      val payloadWithAuth = Json.obj("Authorization" -> "abc")

      manager ! EnrichedSubscriptionRequestV05("sessionId", "projectId", InitConnection(Some(payloadWithAuth)))

      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)
    }

    "fail when the payload contains a NON String value in the Authorization field" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val manager  = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef, bugsnag)))
      val payload1 = Json.obj("Authorization" -> 123)
      manager ! EnrichedSubscriptionRequestV05("sessionId", "projectId", InitConnection(Some(payload1)))

      response05Publisher.expectPublishCount(1, maxWait = 15.seconds)
      response05Publisher.messagesPublished.head.payload shouldBe an[InitConnectionFail]

      val payload2 = Json.obj("Authorization" -> Json.obj())
      manager ! EnrichedSubscriptionRequestV05("sessionId", "projectId", InitConnection(Some(payload2)))

      response05Publisher.expectPublishCount(1, maxWait = 15.seconds)
      response05Publisher.messagesPublished.head.payload shouldBe an[InitConnectionFail]
    }
  }

  "Sending SUBSCRIPTION_START after an INIT" should {
    "respond with SUBSCRIPTION_FAIL when the query is not valid GraphQL" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val manager      = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef, bugsnag)))
      val emptyPayload = Json.obj()

      manager ! enrichedRequest(InitConnection(Some(emptyPayload)))
      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)

      // actual test
      val invalidQuery = // no projection so it is invalid
        """
          | query {
          |   whatever(id: "bla"){}
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(Some("subscription-id"), None)
      val start          = SubscriptionStart(subscriptionId, invalidQuery, variables = None, operationName = None)

      manager ! enrichedRequest(start)

      response05Publisher.expectPublishCount(1, maxWait = 15.seconds)
      response05Publisher.messagesPublished.last.payload shouldBe an[SubscriptionFail]

      val lastResponse = response05Publisher.messagesPublished.last.payload.asInstanceOf[SubscriptionFail]

      lastResponse.id should be(subscriptionId)
      lastResponse.payload.errors.head.message should include("Query was not valid")
    }

    "respond with SUBSCRIPTION_SUCCESS if " +
      "1. the query is valid " +
      "2. the subscriptions manager received CreateSubscription " +
      "3. and the manager responded with CreateSubscriptionSucceeded" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val testProbe    = TestProbe()
      val manager      = system.actorOf(Props(new SubscriptionSessionManager(testProbe.ref, bugsnag)))
      val emptyPayload = Json.obj()

      manager ! enrichedRequest(InitConnection(Some(emptyPayload)))
      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)

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

      manager ! enrichedRequest(start)

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      response05Publisher.expectPublishedMsg(Message("sessionId", SubscriptionSuccess(subscriptionId)), maxWait = 15.seconds)
    }
  }

  "Sending SUBSCRIPTION_END after a SUBSCRIPTION_START" should {
    "result in an EndSubscription message being sent to the subscriptions manager IF a subscription id is supplied" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val testProbe    = TestProbe()
      val manager      = system.actorOf(Props(new SubscriptionSessionManager(testProbe.ref, bugsnag)))
      val emptyPayload = Json.obj()

      manager ! enrichedRequest(InitConnection(Some(emptyPayload)))
      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)

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
      manager ! enrichedRequest(start)

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      response05Publisher.expectPublishedMsg(Message("sessionId", SubscriptionSuccess(subscriptionId)), maxWait = 15.seconds)

      // actual test
      manager ! enrichedRequest(SubscriptionEnd(Some(subscriptionId)))

      val endMsg = testProbe.expectMsgType[EndSubscription]

      endMsg.id should equal(subscriptionId)
      endMsg.projectId should equal("projectId")
    }

    "result in no message being sent to the subscriptions manager IF NO subscription id is supplied" in {
      implicit val response07Publisher = DummyPubSubPublisher[SubscriptionSessionResponse]()
      implicit val response05Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponseV05]()

      val testProbe    = TestProbe()
      val manager      = system.actorOf(Props(new SubscriptionSessionManager(testProbe.ref, bugsnag)))
      val emptyPayload = Json.obj()

      manager ! enrichedRequest(InitConnection(Some(emptyPayload)))
      response05Publisher.expectPublishedMsg(Message("sessionId", InitConnectionSuccess), maxWait = 15.seconds)

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

      manager ! enrichedRequest(start)

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      response05Publisher.expectPublishedMsg(Message("sessionId", SubscriptionSuccess(subscriptionId)), maxWait = 15.seconds)

      // actual test
      manager ! enrichedRequest(SubscriptionEnd(None))
      testProbe.expectNoMessage(3.seconds)
    }
  }

  def enrichedRequest(req: SubscriptionSessionRequestV05): EnrichedSubscriptionRequestV05 =
    EnrichedSubscriptionRequestV05("sessionId", "projectId", req)
}
