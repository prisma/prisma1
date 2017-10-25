package cool.graph.messagebus.testkits

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.pubsub.{Message, Only, Subscription, Topic}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.ClassTag

/**
  * InMemory testkit for simple test cases that requires reasoning over published or received messages.
  * Intercepts all messages transparently.
  */
case class InMemoryPubSubTestKit[T]()(
    implicit tag: ClassTag[T],
    messageTag: ClassTag[Message[T]],
    system: ActorSystem,
    materializer: ActorMaterializer
) extends PubSub[T] {

  val probe             = TestProbe() // Received messages
  val publishProbe      = TestProbe() // Published messages
  val logId             = new java.util.Random().nextInt() // For log output correlation
  var messagesReceived  = Vector.empty[Message[T]]
  var messagesPublished = Vector.empty[Message[T]]
  val _underlying       = InMemoryAkkaPubSub[T]()

  /**
    * For expecting a specific message in the queue with given value in the main queue.
    */
  def expectMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds): Message[T] = probe.expectMsg(maxWait, msg)

  def expectPublishedMessage(msg: Message[T], maxWait: FiniteDuration = 6.seconds): Unit = publishProbe.expectMsg(maxWait, msg)

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
    probe.expectMsgAllClassOf(maxWait, Array.fill(count)(messageTag.runtimeClass): _*)
    probe.expectNoMsg(maxWait)
  }

  def expectPublishCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    publishProbe.expectMsgAllClassOf(maxWait, Array.fill(count)(messageTag.runtimeClass): _*)
    publishProbe.expectNoMsg(maxWait)
  }

  def fishForMessage(msg: Message[T], maxWait: FiniteDuration = 6.seconds) =
    probe.fishForMessage(maxWait) {
      case expected: Message[T] if expected == msg => true
      case _                                       => false
    }

  def fishForPublishedMessage(msg: Message[T], maxWait: FiniteDuration = 6.seconds) =
    publishProbe.fishForMessage(maxWait) {
      case expected: Message[T] if expected == msg => true
      case _                                       => false
    }

  /**
    * Regular publish of messages. Publishes to the main queue.
    */
  def publish(topic: Only, msg: T): Unit = {
    val wrapped = Message(topic.topic, msg)

    synchronized {
      messagesPublished = messagesPublished :+ wrapped
    }

    publishProbe.ref ! wrapped
    _underlying.publish(topic, msg)
  }

  override def shutdown(): Unit = {
    messagesReceived = Vector.empty[Message[T]]
    messagesPublished = Vector.empty[Message[T]]

    _underlying.shutdown

    Await.result(system.terminate(), 10.seconds)
  }

  override def subscribe(topic: Topic, onReceive: (Message[T]) => Unit): Subscription = {
    _underlying.subscribe(
      topic, { msg: Message[T] =>
        println(s"[TestKit][$logId] Received $msg")

        messagesReceived.synchronized {
          messagesReceived = messagesReceived :+ msg
        }

        probe.ref ! msg
        onReceive(msg)
      }
    )
  }

  override def subscribe(topic: Topic, subscriber: ActorRef): Subscription = {
    _underlying.subscribe(
      topic, { msg: Message[T] =>
        println(s"[TestKit][$logId] Received $msg")

        messagesReceived.synchronized {
          messagesReceived = messagesReceived :+ msg
        }

        probe.ref ! msg
        subscriber ! msg
      }
    )
  }

  override def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription = ???

  override def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe
}
