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

class SubscriptionSessionProtocolV07Spec extends TestKit(ActorSystem("subscription-manager-spec")) with WordSpecLike with Matchers with BeforeAndAfterAll {

  import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Requests._
  import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses._

  override def afterAll: Unit = shutdown()

  implicit val materializer  = ActorMaterializer()
  implicit val dependencies  = new TestSubscriptionDependencies
  val ignoreProbe: TestProbe = TestProbe()
  val ignoreRef: ActorRef    = ignoreProbe.testActor

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
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! GqlConnectionInit(Some(emptyPayload))
      parent.expectMsg(GqlConnectionAck)
    }

    "succeed when the payload contains a String in the Authorization field and the project has no secrets" in withProjectFetcherStub(projectId) {
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val payloadWithAuth     = Json.obj("Authorization" -> "abc")

      subscriptionSession ! GqlConnectionInit(Some(payloadWithAuth))
      parent.expectMsg(GqlConnectionAck)
    }
  }

  "Sending GQL_START after an INIT" should {
    "respond with GQL_ERROR when the query is not valid GraphQL" in withProjectFetcherStub(projectId) {
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(ignoreRef)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! GqlConnectionInit(Some(emptyPayload))
      parent.expectMsg(GqlConnectionAck)

      // actual test
      val invalidQuery = // no projection so it is invalid
        """
          | query {
          |   whatever(id: "bla"){}
          | }
        """.stripMargin

      val subscriptionId = StringOrInt(string = Some("subscription-id"), int = None)

      val start = GqlStart(subscriptionId, GqlStartPayload(invalidQuery, variables = None, operationName = None))

      subscriptionSession ! start
      val lastMsg = parent.expectMsgType[GqlError]
      lastMsg.id should be(subscriptionId)
      lastMsg.payload.message should include("Query was not valid")
    }

    "respond with nothing if " +
      "1. the query is valid " +
      "2. the subscriptions manager received CreateSubscription " +
      "3. and the manager responded with CreateSubscriptionSucceeded" in withProjectFetcherStub(projectId) {
      val testProbe           = TestProbe()
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(testProbe.ref)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! GqlConnectionInit(Some(emptyPayload))
      parent.expectMsg(GqlConnectionAck)

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

      subscriptionSession ! start

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))
    }
  }

  "Sending GQL_STOP after a GQL_START" should {
    "result in an EndSubscription message being sent to the subscriptions manager" in withProjectFetcherStub(projectId) {
      val testProbe           = TestProbe()
      val parent              = TestProbe()
      val subscriptionSession = parent.childActorOf(Props(subscriptionSessionActor(testProbe.ref)))
      val emptyPayload        = Json.obj()

      subscriptionSession ! GqlConnectionInit(Some(emptyPayload))
      parent.expectMsg(GqlConnectionAck)

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

      subscriptionSession ! start

      // subscription manager should get request and respond
      testProbe.expectMsgType[CreateSubscription]
      testProbe.reply(CreateSubscriptionSucceeded(CreateSubscription(subscriptionId, null, null, null, null, null, null)))

      // actual test
      subscriptionSession ! GqlStop(subscriptionId)

      val endMsg = testProbe.expectMsgType[EndSubscription]

      endMsg.id should equal(subscriptionId)
      endMsg.projectId should equal("projectId")
    }
  }

  def subscriptionSessionActor(subscriptionsManager: ActorRef) = new SubscriptionSessionActor("sessionId", "projectId", subscriptionsManager)

  def withProjectFetcherStub[T](projectId: String)(fn: => T) = {
    val project = TestProject().copy(id = projectId)
    dependencies.projectFetcher.put(projectId, project)
    fn
  }
}
