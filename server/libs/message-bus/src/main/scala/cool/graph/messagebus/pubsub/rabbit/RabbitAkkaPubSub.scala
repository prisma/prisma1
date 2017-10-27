package cool.graph.messagebus.pubsub.rabbit

import akka.actor.{ActorRef, ActorSystem}
import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller, Converter}
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.{Message, Only, Subscription, Topic}
import cool.graph.messagebus.utils.RabbitUtils

import scala.concurrent.Await

/**
  * Implementation of the PubSub interface that uses rabbit and intermediate actors to handle subscriptions or invoke callbacks.
  * Intermediate actors allow message parsing to be deferred to the latest possible time, and ensures that messages are
  * only parsed if there is an existing subscriber, at the cost of potentially multiple parses of one message, as there
  * is always one intermediate actor per subscription.
  *
  * @param amqpUri Rabbit to connect to.
  * @param exchangeName Underlying exchange name.
  * @param durable Controls whether or not the rabbit exchange is durable. This does NOT control message persistence.
  * @param concurrency Number of concurrent consumer threads
  * @tparam T The type goes over the wire to and from the Rabbit.
  */
case class RabbitAkkaPubSub[T](
    amqpUri: String,
    exchangeName: String,
    durable: Boolean = false,
    concurrency: Int = 1
)(
    implicit val bugSnagger: BugSnagger,
    system: ActorSystem,
    marshaller: ByteMarshaller[T],
    unmarshaller: ByteUnmarshaller[T]
) extends PubSub[T] {
  val exchange   = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency, durable)
  val publisher  = RabbitAkkaPubSubPublisher[T](exchange)
  val subscriber = RabbitAkkaPubSubSubscriber[T](exchange)

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription         = subscriber.subscribe(topic, onReceive)
  def subscribe(topic: Topic, subscriber: ActorRef): Subscription                  = this.subscriber.subscribe(topic, subscriber)
  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]) = this.subscriber.subscribe[U](topic, subscriber, converter)

  def publish(topic: Only, msg: T): Unit            = publisher.publish(topic, msg)
  def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe

  override def shutdown: Unit = {
    subscriber.shutdown()
    exchange.channel.close()
  }
}

/**
  * Collection of convenience standalone initializers and utilities for Rabbit-based pub sub
  */
object RabbitAkkaPubSub {
  def publisher[T](
      amqpUri: String,
      exchangeName: String,
      concurrency: Int = 1,
      durable: Boolean = false
  )(implicit bugSnagger: BugSnagger, marshaller: ByteMarshaller[T]): RabbitAkkaPubSubPublisher[T] = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency, durable)

    RabbitAkkaPubSubPublisher[T](exchange, onShutdown = () => {
      exchange.channel.close().get
    })
  }

  def subscriberWithSystem[T](
      amqpUri: String,
      exchangeName: String,
      concurrency: Int = 1,
      durable: Boolean = false
  )(implicit bugSnagger: BugSnagger, unmarshaller: ByteUnmarshaller[T]): RabbitAkkaPubSubSubscriber[T] = {
    import scala.concurrent.duration._

    implicit val system = SingleThreadedActorSystem("rabbitPubSubSubscriberStandalone")
    val exchange        = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency, durable)

    RabbitAkkaPubSubSubscriber[T](exchange, onShutdown = () => {
      exchange.channel.close().get
      Await.result(system.terminate(), 5.seconds)
    })
  }

  def subscriber[T](
      amqpUri: String,
      exchangeName: String,
      concurrency: Int = 1,
      durable: Boolean = false
  )(implicit bugSnagger: BugSnagger, actorSystem: ActorSystem, unmarshaller: ByteUnmarshaller[T]): RabbitAkkaPubSubSubscriber[T] = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency, durable)

    RabbitAkkaPubSubSubscriber[T](exchange, onShutdown = () => {
      exchange.channel.close().get
    })
  }
}
