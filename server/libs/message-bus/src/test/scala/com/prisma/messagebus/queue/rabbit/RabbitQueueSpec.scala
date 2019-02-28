package com.prisma.messagebus.queue.rabbit

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.prisma.errors.DummyErrorReporter
import com.prisma.messagebus.queue.ConstantBackoff
import com.prisma.messagebus.utils.RabbitUtils
import com.prisma.rabbit.Bindings.RoutingKey
import com.prisma.rabbit.{Consumer, Delivery}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class RabbitQueueSpec
    extends TestKit(ActorSystem("queueing-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  val amqpUri                                          = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI required for testing"))
  implicit val testMarshaller: String => Array[Byte]   = str => str.getBytes("utf-8")
  implicit val testUnmarshaller: Array[Byte] => String = bytes => new String(bytes, Charset.forName("UTF-8"))
  implicit val reporter                                = DummyErrorReporter

  var rabbitQueue: RabbitQueue[String]        = _
  var failingRabbitQueue: RabbitQueue[String] = _
  var testProbe: TestProbe                    = _

  override def afterAll = shutdown(verifySystemShutdown = true)

  override def beforeEach = {
    nukeQueues
    testProbe = TestProbe()

    val testConsumeFn        = (str: String) => { testProbe.ref ! str; Future.successful(()) }
    val testFailingConsumeFn = (str: String) => Future.failed(new Exception("This is expected to happen"))
    val testBackoff          = ConstantBackoff(0.second)

    rabbitQueue = RabbitQueue[String](amqpUri, "test", testBackoff)
    failingRabbitQueue = RabbitQueue[String](amqpUri, "test-failing", testBackoff)

    rabbitQueue.withConsumer(testConsumeFn)
    failingRabbitQueue.withConsumer(testFailingConsumeFn)
  }

  override def afterEach(): Unit = {
    nukeQueues
  }

  // Uncool, but it just has to work now. Sorry!
  def nukeQueues: Unit = {
    Try { rabbitQueue.exchange.rabbitChannel.queueDelete("test-error") }
    Try { rabbitQueue.exchange.rabbitChannel.queueDelete("test-failing-error") }

    Try { rabbitQueue.shutdown }
    Try { failingRabbitQueue.shutdown }

    Try { rabbitQueue.exchange.rabbitChannel.exchangeDelete(rabbitQueue.exchange.name, false) }
    Try { failingRabbitQueue.exchange.rabbitChannel.exchangeDelete(failingRabbitQueue.exchange.name, false) }
  }

  // Plain consumer without any magic (only that exchangeName is suffixed with -exchange) to test specific queueing behaviour
  def plainQueueConsumer(exchangeName: String, queueName: String, binding: String, autoDelete: Boolean, consumeFn: Delivery => Unit): Consumer = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency = 1, durable = false)
    (for {
      queue <- exchange.channel.queueDeclare(queueName, durable = false, autoDelete = autoDelete)
      _     <- queue.bindTo(exchange, RoutingKey(binding))
      consumer <- queue.consume({ d: Delivery =>
                   queue.ack(d)
                   consumeFn(d)
                 })
    } yield consumer).get
  }

  // Note: Publish logic is tested implicitly within the tests.
  "Queue" should {

    "call the onMsg function if a valid message arrives" in {
      val testMsg = "test"

      rabbitQueue.publish(testMsg)
      testProbe.expectMsg(testMsg)
    }

    "increment the message tries correctly on failure" in {
      val testMsg    = "test"
      val errorProbe = TestProbe()

      plainQueueConsumer("test-failing", "test-failing-2", "msg.#", autoDelete = true, (d: Delivery) => {
        errorProbe.ref ! d.envelope.getRoutingKey
      })

      failingRabbitQueue.publish(testMsg)
      errorProbe.expectMsgAllOf(10.seconds, "msg.0", "msg.1", "msg.2", "msg.3", "msg.4", "msg.5")
    }

    "put the message into the error queue if it failed MAX_TRIES (5) times" in {
      val testMsg     = "test"
      val errorQProbe = TestProbe()

      plainQueueConsumer("test-failing", "test-failing-error", "error.#", autoDelete = false, (d: Delivery) => {
        errorQProbe.ref ! d.envelope.getRoutingKey
      })

      failingRabbitQueue.publish(testMsg)
      errorQProbe.expectMsgPF[String](10.seconds) {
        case x: String if x.startsWith("error.5.") => x
      }
    }

    "not process messages with invalid routing key" in {
      rabbitQueue.exchange.publish("not.a.valid.key", "test")
      rabbitQueue.exchange.publish("msg.also.not.a.valid.key", "test")

      // process() will never be called in the consumer
      testProbe.expectNoMessage(6.seconds)
    }

    "requeue with timestamp on backoff > 60s" in {
      val testMsg    = "test"
      val testProbe2 = TestProbe()

      // We don't need that one here, it would only ack off messages and interfere with the setup.
      failingRabbitQueue.shutdown

      // First create a new queue consumer that has a > 60s constant backoff and that always fails messages
      val longBackoffFailingRabbitQueue =
        RabbitQueue[String](amqpUri, "test-failing", ConstantBackoff(61.seconds))(reporter, testMarshaller, testUnmarshaller, system)

      longBackoffFailingRabbitQueue.withConsumer((str: String) => Future.failed(new Exception("This is expected to happen")))

      // This one waits for a message (the requeud one that is) on the main queue
      plainQueueConsumer("test-failing", "test-failing-2", "msg.#", autoDelete = true, (d: Delivery) => {
        testProbe2.ref ! d.envelope.getRoutingKey
      })

      // Publish the test message
      longBackoffFailingRabbitQueue.publish(testMsg)

      // Wait for the requeued message
      testProbe2.fishForMessage(10.seconds) {
        case x: String if rkHasTimestampWithMinDistance(x, 30) => true
        case _                                                 => false
      }

      longBackoffFailingRabbitQueue.exchange.rabbitChannel.exchangeDelete(longBackoffFailingRabbitQueue.exchange.name, false)
    }

    "process messages that have a timestamp in the past" in {
      val timestamp = (System.currentTimeMillis() / 1000) - 1000
      val testMsg   = "test"

      rabbitQueue.exchange.publish(s"msg.1.$timestamp", testMsg)
      testProbe.expectMsg(testMsg)
    }

    /**
      * Checks if a routing key timestamp is present and if the timestamp is at least [distance] seconds away.
      * Positive distance for "in the future", negative for "in the past".
      */
    def rkHasTimestampWithMinDistance(rk: String, distance: Long): Boolean = {
      val components = rk.split("\\.")
      if (components.length != 3) {
        false
      } else {
        val Array(_, _, timestamp) = components

        (timestamp.toLong - (System.currentTimeMillis() / 1000)) > distance
      }
    }
  }
}
