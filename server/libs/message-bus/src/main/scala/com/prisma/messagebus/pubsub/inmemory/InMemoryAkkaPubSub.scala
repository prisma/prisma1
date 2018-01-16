package cool.graph.messagebus.pubsub.inmemory

import akka.actor.{ActorRef, ActorSystem, Props}
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub.PubSubProtocol.Publish
import cool.graph.messagebus.pubsub._

/**
  * PubSub implementation solely backed by actors, no external queueing or pubsub stack is utilized.
  * Useful for the single server solution and tests.
  */
case class InMemoryAkkaPubSub[T]()(implicit val system: ActorSystem) extends PubSub[T] {
  val router = system.actorOf(Props(PubSubRouter()))

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription =
    Subscription(topic.topic, system.actorOf(Props(IntermediateCallbackActor[T, T](topic.topic, router, onReceive)(identity))))

  def subscribe(topic: Topic, subscriber: ActorRef): Subscription =
    Subscription(topic.topic, system.actorOf(Props(IntermediateForwardActor[T, T](topic.topic, router, subscriber)(identity))))

  def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe

  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription =
    Subscription(topic.topic, system.actorOf(Props(IntermediateForwardActor(topic.topic, router, subscriber)(converter))))

  def publish(topic: Only, msg: T): Unit = {
    val message = Message[T](topic.topic, msg)

    router ! Publish(topic.topic, message)
    router ! Publish(Everything.topic, message)
  }

  override def shutdown: Unit = system.stop(router)
}
