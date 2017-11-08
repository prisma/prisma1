package cool.graph.messagebus.pubsub.inmemory

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.routing._
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub._
import cool.graph.messagebus.pubsub.inmemory.PubSubProtocol.Subscribe

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable

/**
  * PubSub implementation solely backed by actors, no external queueing or pubsub stack is utilized.
  * Useful for the single server solution and tests.
  */
case class InMemoryAkkaPubSub[T](implicit val system: ActorSystem) extends PubSub[T] {
  val mediator = DistributedPubSub(system).mediator

  private val _subscriptions = ArrayBuffer[Subscription]()

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription =
    store(Subscription(system.actorOf(Props(IntermediateCallbackActor[T, T](topic.topic, mediator, onReceive)(identity)))))

  def subscribe(topic: Topic, subscriber: ActorRef): Subscription =
    store(Subscription(system.actorOf(Props(IntermediateForwardActor[T, T](topic.topic, mediator, subscriber)(identity)))))

  def unsubscribe(subscription: Subscription): Unit = {
    _subscriptions.synchronized { _subscriptions -= subscription }
    subscription.unsubscribe
  }

  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription =
    store(Subscription(system.actorOf(Props(IntermediateForwardActor(topic.topic, mediator, subscriber)(converter)))))

  private def store(sub: Subscription): Subscription = {
    _subscriptions.synchronized { _subscriptions :+ sub }
    sub
  }

  def publish(topic: Only, msg: T): Unit = {
    val message = Message[T](topic.topic, msg)

    mediator ! Publish(topic.topic, message)
    mediator ! Publish(Everything.topic, message)
  }

  override def shutdown: Unit = {
    _subscriptions.synchronized { _subscriptions.foreach(_.unsubscribe) }
  }
}

object PubSubProtocol {
  case class Subscribe(topic: String, actorRef: ActorRef)
  case class Publish(topic: String, message: Any)
}

case class PubSubRouter() extends Actor {
  val pubSubLogic = PubSubRoutingLogic()
  var router      = Router(pubSubLogic, Vector.empty)

  override def receive: Receive = {
    case Subscribe(topic, ref) =>
      context watch ref
      router = router.addRoutee(ActorRefRoutee(ref))

  }
  // Explicitly don't watch for routee termination for performance?
}

case class PubSubRoutingLogic() extends RoutingLogic {
  import scala.collection.JavaConverters._

  val roundRobin    = RoundRobinRoutingLogic()
  val subscriptions = new ConcurrentHashMap[String, Set[ActorRef]].asScala

  def addSubscriber(topic: String, subscriber: ActorRef) = {}

  def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee = {
    val targets = (1 to nbrCopies).map(_ => roundRobin.select(message, routees))
    SeveralRoutees(targets)
  }
}
