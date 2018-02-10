package com.prisma.subscriptions.protocol

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import com.prisma.messagebus.pubsub.Message
import com.prisma.messagebus.testkits.{DummyPubSubPublisher, InMemoryPubSubTestKit}
import com.prisma.shared.models.ProjectWithClientId
import com.prisma.shared.schema_dsl.TestProject
import com.prisma.stub.Import.withStubServer
import com.prisma.subscriptions.SubscriptionDependenciesForTest
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import com.prisma.subscriptions.protocol.SubscriptionSessionManager.Requests.EnrichedSubscriptionRequest
import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.{CreateSubscription, EndSubscription}
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.CreateSubscriptionSucceeded
import org.scalatest._
import play.api.libs.json.Json

import scala.concurrent.duration._

class SubscriptionSessionManagerProtocolV07Spec
    extends TestKit(ActorSystem("subscription-manager-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Requests._
  import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses._

  override def afterAll: Unit = shutdown()

  implicit val materializer = ActorMaterializer()

  val ignoreProbe: TestProbe = TestProbe()
  val ignoreRef: ActorRef    = ignoreProbe.testActor

  implicit val dependencies = new SubscriptionDependenciesForTest

  def ignoreKeepAliveProbe: TestProbe = {
    val ret = TestProbe()
    ret.ignoreMsg {
      case GqlConnectionKeepAlive => true
    }
    ret
  }

  val projectId = "projectId"

  "Sending an GQL_CONNECTION_INIT message" should {
    "succeed when the payload is empty" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val manager      = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef)))
      val emptyPayload = Json.obj()

      manager ! EnrichedSubscriptionRequest("sessionId", projectId, GqlConnectionInit(Some(emptyPayload)))
      response07Publisher.expectPublishedMsg(Message("sessionId", GqlConnectionAck), maxWait = 15.seconds)
    }

    "succeed when the payload contains a String in the Authorization field" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val manager         = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef)))
      val payloadWithAuth = Json.obj("Authorization" -> "abc")

      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(payloadWithAuth)))
      response07Publisher.expectPublishedMsg(Message("sessionId", GqlConnectionAck), maxWait = 15.seconds)
    }

    "fail when the payload contains a NON String value in the Authorization field" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val manager  = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef)))
      val payload1 = Json.obj("Authorization" -> 123)

      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(payload1)))

      response07Publisher.expectPublishCount(1, maxWait = 15.seconds)
      response07Publisher.messagesPublished.head.payload shouldBe an[GqlConnectionError]

      val payload2 = Json.obj("Authorization" -> Json.obj())
      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(payload2)))

      response07Publisher.expectPublishCount(1, maxWait = 15.seconds)
      response07Publisher.messagesPublished.last.payload shouldBe an[GqlConnectionError]
    }
  }

  "Sending GQL_START after an INIT" should {
    "respond with GQL_ERROR when the query is not valid GraphQL" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val manager      = system.actorOf(Props(new SubscriptionSessionManager(ignoreRef)))
      val emptyPayload = Json.obj()

      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(emptyPayload)))
      response07Publisher.expectPublishedMsg(Message("sessionId", GqlConnectionAck), maxWait = 15.seconds)

      // actual test
      val invalidQuery = // no projection so it is invalid
        """
          | query {
          |   whatever(id: "bla"){}
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(string = Some("subscription-id"), int = None)

      val start = GqlStart(subscriptionId, GqlStartPayload(invalidQuery, variables = None, operationName = None))

      manager ! enrichedRequest(start)
      response07Publisher.expectPublishCount(1, maxWait = 15.seconds)

      val lastMsg = response07Publisher.messagesPublished.last

      lastMsg.payload shouldBe an[GqlError]
      lastMsg.payload.asInstanceOf[GqlError].id should be(subscriptionId)
      lastMsg.payload.asInstanceOf[GqlError].payload.message should include("Query was not valid")
    }

    "respond with nothing if " +
      "1. the query is valid " +
      "2. the subscriptions manager received CreateSubscription " +
      "3. and the manager responded with CreateSubscriptionSucceeded" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val testProbe    = TestProbe()
      val manager      = system.actorOf(Props(new SubscriptionSessionManager(testProbe.ref)))
      val emptyPayload = Json.obj()

      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(emptyPayload)))
      response07Publisher.expectPublishedMsg(Message("sessionId", GqlConnectionAck), maxWait = 15.seconds)

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
      val start          = GqlStart(subscriptionId, GqlStartPayload(validQuery, variables = None, operationName = None))

      manager ! enrichedRequest(start)

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      // FIXME: expect no message here??
    }
  }

  "Sending GQL_STOP after a GQL_START" should {
    "result in an EndSubscription message being sent to the subscriptions manager" in withProjectFetcherStub(projectId) {
      implicit val response07Publisher = InMemoryPubSubTestKit[SubscriptionSessionResponse]()
      implicit val response05Publisher = DummyPubSubPublisher[SubscriptionSessionResponseV05]()

      val testProbe    = TestProbe()
      val manager      = system.actorOf(Props(new SubscriptionSessionManager(testProbe.ref)))
      val emptyPayload = Json.obj()

      manager ! EnrichedSubscriptionRequest("sessionId", "projectId", GqlConnectionInit(Some(emptyPayload)))
      response07Publisher.expectPublishedMsg(Message("sessionId", GqlConnectionAck), maxWait = 15.seconds)

      val validQuery =
        """
          | query {
          |   whatever(id: "bla"){
          |     id
          |   }
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(string = Some("subscription-id"), int = None)
      val start          = GqlStart(subscriptionId, GqlStartPayload(validQuery, variables = None, operationName = None))

      manager ! enrichedRequest(start)

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      // FIXME: expect no message here??
      //responseExchange.expectInvocation(SubscriptionSuccess(subscriptionId))

      // actual test
      manager ! enrichedRequest(GqlStop(subscriptionId))

      val endMsg = testProbe.expectMsgType[EndSubscription]

      endMsg.id should equal(subscriptionId)
      endMsg.projectId should equal("projectId")
    }
  }

  def withProjectFetcherStub[T](projectId: String)(fn: => T) = {
    import com.prisma.shared.models.ProjectJsonFormatter._
    val project             = TestProject().copy(id = projectId)
    val projectWithClientId = ProjectWithClientId(project)
    val stubs = List(
      com.prisma.stub.Import.Request("GET", s"/${dependencies.projectFetcherPath}/${project.id}").stub(200, Json.toJson(projectWithClientId).toString)
    )
    withStubServer(stubs, port = dependencies.projectFetcherPort) {
      fn
    }
  }

  def enrichedRequest(req: SubscriptionSessionRequest): EnrichedSubscriptionRequest =
    EnrichedSubscriptionRequest("sessionId", "projectId", req)
}
