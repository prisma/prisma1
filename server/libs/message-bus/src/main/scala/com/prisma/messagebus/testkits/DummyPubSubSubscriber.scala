package com.prisma.messagebus.testkits

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.messagebus.Conversions.Converter
import com.prisma.messagebus.{PubSubPublisher, PubSubSubscriber}
import com.prisma.messagebus.pubsub.{Message, Only, Subscription, Topic}

object DummyPubSubSubscriber {
  // Initializes a minimal actor system to use
  def standalone[T]: DummyPubSubSubscriber[T] = {
    implicit val system = SingleThreadedActorSystem("DummyPubSubSubscriber")
    DummyPubSubSubscriber[T]()
  }
}

case class DummyPubSubSubscriber[T]()(implicit system: ActorSystem) extends PubSubSubscriber[T] {
  val testProbe = TestProbe()

  override def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription         = Subscription(topic.topic, testProbe.ref)
  override def subscribe(topic: Topic, subscriber: ActorRef): Subscription                  = Subscription(topic.topic, testProbe.ref)
  override def unsubscribe(subscription: Subscription): Unit                                = {}
  override def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]) = Subscription(topic.topic, testProbe.ref)
}

case class DummyPubSubPublisher[T]() extends PubSubPublisher[T] {
  override def publish(topic: Only, msg: T): Unit = {}
}
