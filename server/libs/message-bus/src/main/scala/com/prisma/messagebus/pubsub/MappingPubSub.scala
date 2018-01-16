package cool.graph.messagebus.pubsub

import akka.actor.ActorRef
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.{PubSubPublisher, PubSubSubscriber}

/**
  * PubSubSubscriber decorator that allows subscribers to transparently subscribe to a different type using the given
  * converter to map the original subscription type to the newly expected type.
  */
case class MappingPubSubSubscriber[A, B](pubSubSubscriber: PubSubSubscriber[A], converter: Converter[A, B]) extends PubSubSubscriber[B] {
  override def subscribe(topic: Topic, onReceive: (Message[B]) => Unit): Subscription =
    pubSubSubscriber.subscribe(topic, onReceive = theA => onReceive(theA.map(converter)))

  override def subscribe(topic: Topic, subscriber: ActorRef): Subscription = pubSubSubscriber.subscribe(topic, subscriber, converter)

  override def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[B, U]) =
    pubSubSubscriber.subscribe(topic, subscriber, this.converter.andThen(converter))

  override def unsubscribe(subscription: Subscription): Unit = pubSubSubscriber.unsubscribe(subscription)
}

/**
  * PubSubPublisher decorator that allows publishers to transparently publish a different type using the given
  * converter to map the original publish type to the type of the underlying publisher.
  */
case class MappingPubSubPublisher[B, A](queuePublisher: PubSubPublisher[A], converter: Converter[B, A]) extends PubSubPublisher[B] {
  override def publish(topic: Only, msg: B): Unit = queuePublisher.publish(topic: Only, converter(msg))
}
