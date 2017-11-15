package cool.graph.messagebus.testkits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.messagebus.queue.{BackoffStrategy, ConstantBackoff}
import cool.graph.messagebus.{ConsumerRef, Queue}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.ClassTag

/**
  * InMemory testkit for simple test cases that requires reasoning over published or received messages for a Queue.
  * Intercepts all messages transparently.
  *
  * The overall interface is intentionally close to akka testkit, and leverages TestProbes internally
  * to use and combine akka testkit calls to reason over queue messages.
  *
  * Messages published to the queue and received by the registered consumers are stored in separate collections
  * messagesReceived and messagesPublished. Each expect call on this testkit has a version for
  * for published messages and for incoming messages.
  */
case class InMemoryQueueTestKit[T](backoff: BackoffStrategy = ConstantBackoff(1.second))(
    implicit tag: ClassTag[T],
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Queue[T] {
  import system.dispatcher

  /**
    * Why that much mutable state?
    * This is a simple workaround for our issue that scaldi dependencies are create once _per test file_
    * and message state is bleeding over in between tests (i.e. we need to reset the testkit), which requires us
    * to either build more sophisticated dependency tooling or a more complex implementation to encapsulate this
    * state separately. Both solutions require more time than is currently available.
    */
  var probe             = TestProbe() // Receives messages
  var publishProbe      = TestProbe() // Receives published messages
  val logId             = new java.util.Random().nextInt(Integer.MAX_VALUE) // For log output correlation
  var _underlying       = InMemoryAkkaQueue[T]()
  val messagesReceived  = new ArrayBuffer[T]()
  val messagesPublished = new ArrayBuffer[T]()

  /**
    * Registers the standard test consumer that just stores the incoming messages in messagesReceived and notifies the
    * consumer probe.
    *
    * This is usually used in test cases that test code without consumer registration.
    */
  def withTestConsumer(): Unit = {
    withConsumer { _ =>
      Future.successful(())
    }
  }

  /**
    * Registers a custom consumer that will be invoked on message receive.
    * The consumer is wrapped transparently by the necessary logic to store messages and notify the test probe, so the
    * consume callback does not need to implement that logic. Hence, all expect calls will just work as usual with the
    * custom consumer.
    *
    * This is usually used in test cases that test code without consumer registration, which require custom logic for
    * processing messages in tests.
    */
  override def withConsumer(fn: ConsumeFn[T]): ConsumerRef = {
    _underlying.withConsumer { msg: T =>
      println(s"[QTestKit][$logId] Received message: $msg")
      probe.ref ! msg

      messagesReceived.synchronized {
        messagesReceived += msg
      }

      fn(msg)
    }
  }

  /**
    * For expecting a specific message to arrive _at any consumer_ in the given time frame.
    * Requires at least one consumer to be meaningful.
    */
  def expectMsg(msg: T, maxWait: FiniteDuration = 6.seconds): T = probe.expectMsg(maxWait, msg)

  /**
    * For expecting a specific message to be published to the queue in the given time frame.
    * Does not require a consumer to be meaningful.
    */
  def expectPublishedMsg(msg: T, maxWait: FiniteDuration = 6.seconds): Unit = publishProbe.expectMsg(maxWait, msg)

  /**
    * For expecting no message to arrive _at any consumer_ in the given time frame.
    * Requires at least one consumer to be meaningful.
    */
  def expectNoMsg(maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectNoMsg(maxWait)
  }

  /**
    * For expecting no message to be published to the queue in the given time frame.
    * Does not require a consumer to be meaningful.
    */
  def expectNoPublishedMsg(maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectNoMsg(maxWait)
  }

  /**
    * Expects a number of messages to arrive _at any consumer_ (count is across all subscribers).
    * Matches the exact count on the messages received in the given time frame, meaning that too many or less
    * messages than expected will result in a failure.
    *
    * Requires at least one consumer to be meaningful.
    */
  def expectMsgCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    probe.expectNoMsg(maxWait)
  }

  /**
    * Expects a number of messages to be published to this queue.
    * Matches the exact count on the messages published in the given time frame, meaning that too many or less
    * messages than expected will result in a failure.
    *
    * Does not require a consumer to be meaningful.
    */
  def expectPublishCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    publishProbe.expectNoMsg(maxWait)
  }

  /**
    * Waits for a specific message to arrive _at any of the registered consumers_ for a maximum of maxWait duration.
    * Requires at least one consumer to be meaningful.
    */
  def fishForMsg(msg: T, maxWait: FiniteDuration = 6.seconds) =
    probe.fishForMessage(maxWait) {
      case expected: T if expected == msg => true
      case _                              => false
    }

  /**
    * Waits for a specific message to be published to this queue for a maximum of maxWait duration.
    * Does not require a consumer to be meaningful.
    */
  def fishForPublishedMsg(msg: T, maxWait: FiniteDuration = 6.seconds) =
    publishProbe.fishForMessage(maxWait) {
      case expected: T if expected == msg => true
      case _                              => false
    }

  /**
    * Publish a message to this queue.
    */
  def publish(msg: T): Unit = {
    println(s"[QTestKit][$logId] Published: $msg")
    messagesPublished.synchronized { messagesPublished += msg }
    publishProbe.ref ! msg
    _underlying.publish(msg)
  }

  def reset: Unit = {
    messagesReceived.clear()
    messagesPublished.clear()
    probe = TestProbe()
    publishProbe = TestProbe()
  }

  override def shutdown(): Unit = {
    reset
    _underlying.shutdown
  }
}
