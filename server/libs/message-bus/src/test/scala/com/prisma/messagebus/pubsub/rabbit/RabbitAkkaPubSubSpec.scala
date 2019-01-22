package com.prisma.messagebus.pubsub.rabbit

import akka.testkit.{TestKit, TestProbe}
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.errors.DummyErrorReporter
import com.prisma.messagebus.Conversions
import com.prisma.messagebus.pubsub.{Everything, Message, Only}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

class RabbitAkkaPubSubSpec
    extends TestKit(SingleThreadedActorSystem("pubsub-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  override def afterAll = shutdown(verifySystemShutdown = true)

  val amqpUri           = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI required for testing"))
  implicit val reporter = DummyErrorReporter

  val testTopic = Only("testTopic")
  val testMsg   = "testMsg"

  implicit val testMarshaller   = Conversions.Marshallers.FromString
  implicit val testUnmarshaller = Conversions.Unmarshallers.ToString

  def doTest(testFn: (RabbitAkkaPubSub[String], TestProbe) => _) = {
    val probe  = TestProbe()
    val pubSub = RabbitAkkaPubSub[String](amqpUri, "testExchange")

    waitForInit(pubSub)

    try {
      testFn(pubSub, probe)
    } finally {
      pubSub.shutdown
    }
  }

  // Wait for the consumer to bind and the mediator to init
  def waitForInit[T](pubSub: RabbitAkkaPubSub[T]): Unit = {
    pubSub.subscriber.consumer.isSuccess
    pubSub.subscriber.router
  }

  // Note: Publish logic is tested implicitly within the subscribe tests.
  // Additionally: Thread.sleep is used to give the subscriber time to bind before messages are published
  "PubSub" should {

    /**
      * Callback tests
      */
    "call the specified callback if a message for the subscription arrives" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val testCallback = (msg: Message[String]) => probe.ref ! msg
        val subscription = pubSub.subscribe(testTopic, testCallback)

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        probe.expectMsg(Message[String](testTopic.topic, testMsg))
      }
    }

    "not call the specified callback if the message doesn't match" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val testCallback = (msg: Message[String]) => probe.ref ! msg
        val subscription = pubSub.subscribe(Only("NOPE"), testCallback)

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        probe.expectNoMessage(6.seconds)
      }
    }

    "not call the specified callback if the subscriber unsubscribed" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val testCallback = (msg: Message[String]) => probe.ref ! msg
        val subscription = pubSub.subscribe(testTopic, testCallback)

        Thread.sleep(500)

        pubSub.unsubscribe(subscription)
        pubSub.publish(testTopic, testMsg)
        probe.expectNoMessage(6.seconds)
      }
    }

    "send the unmarshalled messages from different topics to the given callback when subscribed to everything" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val testCallback = (msg: Message[String]) => probe.ref ! msg
        val subscription = pubSub.subscribe(Everything, testCallback)
        val testMsg2     = "testMsg2"

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        pubSub.publish(Only("testTopic2"), testMsg2)
        probe.expectMsgAllOf(Message[String](testTopic.topic, testMsg), Message[String]("testTopic2", testMsg2))
      }
    }

    /**
      * Actor tests
      */
    "send the unmarshalled message to the given actor" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val subscription = pubSub.subscribe(testTopic, probe.ref)

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        probe.expectMsg(Message[String](testTopic.topic, testMsg))
      }
    }

    "not send the message to the given actor if the message doesn't match" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val subscription = pubSub.subscribe(Only("NOPE"), probe.ref)

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        probe.expectNoMessage(6.seconds)
      }
    }

    "not send the message to the given actor if the subscriber unsubscribed" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val subscription = pubSub.subscribe(testTopic, probe.ref)

        Thread.sleep(500)

        pubSub.unsubscribe(subscription)
        pubSub.publish(testTopic, testMsg)
        probe.expectNoMessage(6.seconds)
      }
    }

    "send the unmarshalled messages from different topics to the given actor when subscribed to everything" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val subscription = pubSub.subscribe(Everything, probe.ref)
        val testMsg2     = "testMsg2"

        Thread.sleep(500)

        pubSub.publish(testTopic, testMsg)
        pubSub.publish(Only("testTopic2"), testMsg2)
        probe.expectMsgAllOf(Message[String](testTopic.topic, testMsg), Message[String]("testTopic2", testMsg2))
      }
    }

    "remap the message to a different type if used with a mapping pubSub subscriber" in {
      doTest { (pubSub: RabbitAkkaPubSub[String], probe: TestProbe) =>
        val msg       = "1234"
        val converter = (s: String) => s.toInt
        val newPubSub = pubSub.map[Int](converter)
        val newProbe  = TestProbe()

        pubSub.subscribe(testTopic, probe.ref)
        newPubSub.subscribe(testTopic, newProbe.ref)

        Thread.sleep(500)

        pubSub.publish(testTopic, msg)

        probe.expectMsg(Message[String](testTopic.topic, msg))
        newProbe.expectMsg(Message[Int](testTopic.topic, 1234))
      }
    }
  }
}
