package cool.graph.messagebus.testkits

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub._
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub

import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.ClassTag

/**
  * InMemory testkit for simple test cases that requires reasoning over published or received messages for a PubSub.
  * Intercepts all messages transparently.
  *
  * The overall interface is intentionally close to akka testkit, and leverages TestProbes internally
  * to use and combine akka testkit calls to reason over pub sub messages.
  *
  * Messages published to the pubsub and received by the subscribers are stored in separate collections
  * messagesReceived and messagesPublished. Each expect call on this testkit has a version for
  * for published messages and for received messages.
  *
  * Important: Messages received by multiple subscribers (e.g. they have the same topic subscription) will intentionally
  * have multiple copies in the messages list - no message dedup is taking place.
  *
  * Subscribing requires a minimal delay for the actors to set up or else messages might slip through before subscriptions
  * are fully set up - hence the Thread.sleep calls.
  */
case class InMemoryPubSubTestKit[T]()(
    implicit tag: ClassTag[T],
    messageTag: ClassTag[Message[T]],
    system: ActorSystem,
    materializer: ActorMaterializer
) extends PubSub[T] {

  var probe             = TestProbe() // Received messages
  var publishProbe      = TestProbe() // Published messages
  val logId             = new java.util.Random().nextInt(Integer.MAX_VALUE) // For log output correlation
  var messagesReceived  = Vector.empty[Message[T]]
  var messagesPublished = Vector.empty[Message[T]]
  val _underlying       = InMemoryAkkaPubSub[T]()

  /**
    * Subscribes a dummy test subscriber that listens to all messages on the PubSub, stores them
    * in the messagesReceived, and notifies the probe.
    */
  def withTestSubscriber: Subscription = {
    val sub = subscribe(Everything, msg => /* noop */ ())
    Thread.sleep(50)
    sub
  }

  /**
    * Subscribes a custom callback that will be invoked when a message arrives for the given topic.
    * The subscriber is wrapped transparently by the necessary logic to store messages and notify the test probe, so the
    * callback does not need to implement that logic. Hence, all expect calls will just work as usual with the
    * custom subscriber.
    *
    * This is usually used in test cases that test code without subscribers, which require custom logic for
    * processing messages in tests.
    */
  override def subscribe(topic: Topic, onReceive: (Message[T]) => Unit): Subscription = {
    val sub = _underlying.subscribe(
      topic,
      (msg: Message[T]) => {
        println(s"[TestKit][$logId] Received $msg")

        messagesReceived.synchronized {
          messagesReceived = messagesReceived :+ msg
        }

        probe.ref ! msg
        onReceive(msg)
      }
    )

    Thread.sleep(50)
    sub
  }

  /**
    * Subscribes a custom actor that will receive incoming message for the given topic.
    * The subscriber is wrapped transparently by the necessary logic to store messages and notify the test probe, so the
    * actor does not need to implement that logic. Hence, all expect calls will just work as usual with the
    * custom actor.
    *
    * This is usually used in test cases that test code without subscribers, which require custom logic for
    * processing messages in tests.
    */
  override def subscribe(topic: Topic, subscriber: ActorRef): Subscription = {
    val sub = _underlying.subscribe(
      topic, { msg: Message[T] =>
        println(s"[TestKit][$logId] Received $msg")

        messagesReceived.synchronized {
          messagesReceived = messagesReceived :+ msg
        }

        probe.ref ! msg
        subscriber ! msg
      }
    )

    Thread.sleep(50)
    sub
  }

  /**
    * For expecting a specific message to arrive at subscribers in the PubSub with given value.
    * Requires at least one subscriber to be meaningful.
    */
  def expectMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds): Message[T] = probe.expectMsg(maxWait, msg)

  /**
    * For expecting a specific message to be published to the PubSub with given value.
    * Does _not_ require a subscriber to be meaningful.
    */
  def expectPublishedMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds): Unit = publishProbe.expectMsg(maxWait, msg)

  /**
    * For expecting that no message arrived _at any subscriber_ in the given time frame.
    * Requires at least one subscriber to be meaningful.
    */
  def expectNoMsg(maxWait: FiniteDuration = 6.seconds): Unit = probe.expectNoMessage(maxWait)

  /**
    * For expecting that no message was published to the PubSub in the given time frame.
    * Does _not_ require a subscriber to be meaningful.
    */
  def expectNoPublishedMsg(maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectNoMessage(maxWait)
  }

  /**
    * Expects a number of messages to arrive _at any subscriber_ in the given time frame (count is across all subscribers).
    * Matches the total count received in the time frame, so too many messages results in a failure.
    *
    * Requires at least one subscriber to be meaningful.
    */
  def expectMsgCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectMsgAllClassOf(maxWait, Array.fill(count)(messageTag.runtimeClass): _*)
    probe.expectNoMessage(maxWait)
  }

  /**
    * Expects a number of messages to be published to the PubSub in the given time frame.
    * Matches the total count received in the time frame, so too many messages results in a failure.
    *
    * Does _not_ require a subscriber to be meaningful.
    */
  def expectPublishCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectMsgAllClassOf(maxWait, Array.fill(count)(messageTag.runtimeClass): _*)
    publishProbe.expectNoMessage(maxWait)
  }

  /**
    * Waits for a specific message to arrive _at any subscriber_ for a maximum of maxWait duration.
    * Requires at least one subscriber to be meaningful.
    */
  def fishForMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds) =
    probe.fishForMessage(maxWait) {
      case expected: Message[T] if expected == msg => true
      case _                                       => false
    }

  /**
    * Waits for a specific message to be published to this queue for a maximum of maxWait duration.
    * Does not require a subscriber to be meaningful.
    */
  def fishForPublishedMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds) =
    publishProbe.fishForMessage(maxWait) {
      case expected: Message[T] if expected == msg => true
      case _                                       => false
    }

  /**
    * Publish a message to this pubsub.
    */
  def publish(topic: Only, msg: T): Unit = {
    val wrapped = Message(topic.topic, msg)

    synchronized {
      messagesPublished = messagesPublished :+ wrapped
    }

    publishProbe.ref ! wrapped
    _underlying.publish(topic, msg)
  }

  def reset: Unit = {
    messagesReceived = Vector.empty[Message[T]]
    messagesPublished = Vector.empty[Message[T]]
    probe = TestProbe()
    publishProbe = TestProbe()
  }

  override def shutdown(): Unit = {
    reset
    _underlying.shutdown
  }

  override def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription = {
    val sub = _underlying.subscribe(
      topic, { msg: Message[T] =>
        println(s"[TestKit][$logId] Received $msg")

        messagesReceived.synchronized {
          messagesReceived = messagesReceived :+ msg
        }

        val convertedMsg = Message[U](msg.topic, converter(msg.payload))

        probe.ref ! msg
        subscriber ! convertedMsg
      }
    )

    Thread.sleep(50)
    sub
  }

  override def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe
}
