package cool.graph.messagebus.pubsub.inmemory

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub._

/**
  * PubSub implementation solely backed by actors, no external queueing or pubsub stack is utilized.
  * Useful for the single server solution and tests.
  */
case class InMemoryAkkaPubSub[T](implicit val system: ActorSystem) extends PubSub[T] {
  lazy val mediator = DistributedPubSub(system).mediator

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription =
    Subscription(system.actorOf(Props(IntermediateCallbackActor[T, T](topic.topic, mediator, onReceive)(identity))))

  def subscribe(topic: Topic, subscriber: ActorRef): Subscription =
    Subscription(system.actorOf(Props(IntermediateForwardActor[T, T](topic.topic, mediator, subscriber)(identity))))

  def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe

  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription =
    Subscription(system.actorOf(Props(IntermediateForwardActor(topic.topic, mediator, subscriber)(converter))))

  def publish(topic: Only, msg: T): Unit = {
    val message = Message[T](topic.topic, msg)

    mediator ! Publish(topic.topic, message)
    mediator ! Publish(Everything.topic, message)
  }
}
