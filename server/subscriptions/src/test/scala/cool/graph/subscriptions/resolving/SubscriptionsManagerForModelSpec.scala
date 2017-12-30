package cool.graph.subscriptions.resolving

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import cool.graph.bugsnag.{BugSnagger, BugSnaggerMock}
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.ModelMutationType
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.subscriptions.SubscriptionDependenciesForTest
import cool.graph.subscriptions.protocol.StringOrInt
import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import cool.graph.subscriptions.resolving.SubscriptionsManager.Responses.SubscriptionEvent
import cool.graph.subscriptions.resolving.SubscriptionsManagerForModel.Requests.StartSubscription
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

  implicit val materializer = ActorMaterializer()
  implicit val dependencies = new SubscriptionDependenciesForTest()
  //val testDatabase                 = new SimpleTestDatabase
  implicit val bugsnag: BugSnagger = BugSnaggerMock

  val testQuery = QueryParser.parse("""
                                      |subscription {
                                      |  Todo {
                                      |    node {
                                      |      text
                                      |    }
                                      |  }
                                      |}
                                    """.stripMargin).get

  val schema = SchemaDsl.schema()
  val todo   = schema.model("Todo").field("text", _.String)

  val project   = schema.buildProject()
  val todoModel = project.models.find(_.name == "Todo").get

  "subscribing two times with the same subscription id but different session ids" should {
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
              todoModel,
              bugsnag
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
        s"""{"nodeId":"test-node-id","modelId":"${todoModel.id}","mutationType":"CreateNode"}"""
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
        s"""{"nodeId":"test-node-id","modelId":"${todoModel.id}","mutationType":"CreateNode"}"""
      )

      subscriber1.expectNoMessage(5.seconds)
      subscriber2.expectMsg(SubscriptionEvent(subscriptionId, subscriptionDataJson))
    }
  }
}
