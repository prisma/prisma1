package cool.graph.messagebus.testkits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.messagebus.queue.{BackoffStrategy, ConstantBackoff}
import cool.graph.messagebus.{ConsumerRef, Queue}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.existentials
import scala.reflect.ClassTag

/**
  * InMemory testkit for simple test cases that requires reasoning over published or received messages.
  */
case class InMemoryQueueTestKit[T](backoff: BackoffStrategy = ConstantBackoff(1.second))(
    implicit tag: ClassTag[T],
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Queue[T] {
  import system.dispatcher

  val probe             = TestProbe() // Receives messages
  val publishProbe      = TestProbe() // Receives published messages
  val logId             = new java.util.Random().nextInt() // For log output correlation
  var messagesReceived  = Vector.empty[T]
  var messagesPublished = Vector.empty[T]
  val _underlying       = InMemoryAkkaQueue[T]()

  def withTestConsumer(): Unit = {
    _underlying
      .withConsumer { msg: T =>
        Future {
          println(s"[TestKit][$logId] Received $msg")

          probe.ref ! msg

          messagesReceived.synchronized {
            messagesReceived = messagesReceived :+ msg
          }
        }
      }
  }

  override def withConsumer(fn: ConsumeFn[T]): ConsumerRef = {
    _underlying.withConsumer { msg: T =>
      probe.ref ! msg

      messagesReceived.synchronized {
        messagesReceived = messagesReceived :+ msg
      }

      fn(msg)
    }
  }

  /**
    * For expecting a specific message in the queue with given value in the main queue.
    */
  def expectMsg(msg: T, maxWait: FiniteDuration = 6.seconds): T = probe.expectMsg(maxWait, msg)

  def expectPublishedMessage(msg: T, maxWait: FiniteDuration = 6.seconds): Unit = publishProbe.expectMsg(maxWait, msg)

  /**
    * For expecting no message in the given timeframe.
    */
  def expectNoMsg(maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectNoMsg(maxWait)
  }

  def expectNoPublishedMessage(maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectNoMsg(maxWait)
  }

  /**
    * Expects a number of messages to arrive.
    * Matches the total count received in the time frame, so too many messages is also a failure.
    */
  def expectMsgCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    probe.expectNoMsg(maxWait)
  }

  def expectPublishCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    publishProbe.expectNoMsg(maxWait)
  }

  def fishForMessage(msg: T, maxWait: FiniteDuration = 6.seconds) =
    probe.fishForMessage(maxWait) {
      case expected: T if expected == msg => true
      case _                              => false
    }

  def fishForPublishedMessage(msg: T, maxWait: FiniteDuration = 6.seconds) =
    publishProbe.fishForMessage(maxWait) {
      case expected: T if expected == msg => true
      case _                              => false
    }

  /**
    * Regular publish of messages. Publishes to the main queue.
    */
  def publish(msg: T): Unit = {
    synchronized { messagesPublished = messagesPublished :+ msg }
    publishProbe.ref ! msg
    _underlying.publish(msg)
  }

  override def shutdown(): Unit = {
    messagesReceived = Vector.empty[T]
    messagesPublished = Vector.empty[T]

    _underlying.shutdown

    Await.result(system.terminate(), 10.seconds)
  }
}
