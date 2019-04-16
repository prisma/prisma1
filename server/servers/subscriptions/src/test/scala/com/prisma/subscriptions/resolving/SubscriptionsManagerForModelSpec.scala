package com.prisma.subscriptions.resolving

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.ModelMutationType
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.subscriptions.TestSubscriptionDependencies
import com.prisma.subscriptions.protocol.StringOrInt
import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.SubscriptionEvent
import com.prisma.subscriptions.resolving.SubscriptionsManagerForModel.Requests.StartSubscription
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}
import sangria.parser.QueryParser

import scala.concurrent.Future

class SubscriptionsManagerForModelSpec
    extends TestKit(ActorSystem("subscription-manager-for-model-mutation-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  import scala.concurrent.duration._

  override def afterAll = shutdown()

  implicit val materializer         = ActorMaterializer()
  implicit val dependencies         = new TestSubscriptionDependencies()
  implicit lazy val implicitSuite   = this
  implicit lazy val deployConnector = dependencies.deployConnector

  val testQuery = QueryParser.parse("""
                                      |subscription {
                                      |  Todo {
                                      |    node {
                                      |      text
                                      |    }
                                      |  }
                                      |}
                                    """.stripMargin).get

  val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |  id: ID! @id
      |  text: String
      |}
      |
    """.stripMargin
  }
  val todoModel = project.models.find(_.name == "Todo").get

  "subscribing two times with the same subscription id but different session ids" ignore {
    "result in 2 active subscriptions" in {
      val subscriber1          = TestProbe()
      val subscriber2          = TestProbe()
      val pubSub               = dependencies.sssEventsPublisher
      val subscriptionDataJson = Json.parse("""
                              |{
                              |  "data": {
                              |    "Todo": {
                              |      "message": "this worked!"
                              |    }
                              |  }
                              |}
                            """.stripMargin)

      val invocationCount = new AtomicLong(0)
      val manager = {
        system.actorOf(
          Props {
            new SubscriptionsManagerForModel(
              project,
              todoModel
            ) {
              override def processDatabaseEventForSubscription(
                  event: String,
                  subscription: StartSubscription,
                  mutationType: ModelMutationType
              ): Future[Option[JsValue]] = {
                invocationCount.addAndGet(1)
                Future.successful(Some(subscriptionDataJson))
              }
            }
          },
          "subscriptions-manager"
        )
      }

      val subscriptionId = StringOrInt(string = Some("subscription-id"), int = None)

      manager ! StartSubscription(
        id = subscriptionId,
        sessionId = "session1",
        query = testQuery,
        variables = None,
        operationName = None,
        mutationTypes = Set(ModelMutationType.Created),
        subscriber = subscriber1.testActor
      )

      manager ! StartSubscription(
        id = subscriptionId,
        sessionId = "session2",
        query = testQuery,
        variables = None,
        operationName = None,
        mutationTypes = Set(ModelMutationType.Created),
        subscriber = subscriber2.testActor
      )

      Thread.sleep(100)

      pubSub.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"test-node-id","modelId":"${todoModel.name}","mutationType":"CreateNode"}"""
      )

      // both receive the subscription data
      subscriber1.expectMsg(SubscriptionEvent(subscriptionId, subscriptionDataJson))
      subscriber2.expectMsg(SubscriptionEvent(subscriptionId, subscriptionDataJson))

      // the queries were the same so the manager must optimize and only execute the query once
      invocationCount.get() should be(1)

      // after one ends the subscription, the other keeps working
      manager ! EndSubscription(id = subscriptionId, sessionId = "session1", projectId = "not important here")

      pubSub.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"test-node-id","modelId":"${todoModel.name}","mutationType":"CreateNode"}"""
      )

      subscriber1.expectNoMessage(5.seconds)
      subscriber2.expectMsg(SubscriptionEvent(subscriptionId, subscriptionDataJson))
    }
  }
}
