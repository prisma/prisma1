package cool.graph.messagebus.pubsub.rabbit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.{ByteUnmarshaller, Converter}
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub._
import cool.graph.messagebus.utils.Utils
import cool.graph.rabbit.Bindings.FanOut
import cool.graph.rabbit.Import.Exchange

import scala.util.{Failure, Success}

/**
  * A Subscriber subscribes given actors or callbacks to a specific topic.
  * The PubSubSubscriber encapsulates the rabbit consumer required for making our pubsub pattern work:
  * - The assumption is that one exchange is responsible for one type T of message that gets published.
  *
  * - Rabbit acts as a fast, but dumb, pipe: There is one queue bound to the exchange from this code that receives all
  *   messages published to the exchange ("FanOut" == "#" rabbit routing key == receive all).
  *
  * - One actor, the mediator, takes all messages and publishes those based on their routing key.
  *
  * - Additionally, the mediator takes all messages and published them to all "all" subscribers.
  *
  * - No message parsing takes place, as this would result in massive overhead. This is deferred to the intermediate actors.
  *
  * - Each subscription has one intermediate actor that parses the messages and then invokes callbacks or forwards to other actors.
  */
case class RabbitAkkaPubSubSubscriber[T](
    exchange: Exchange,
    onShutdown: () => Unit = () => ()
)(
    implicit val bugSnagger: BugSnagger,
    val system: ActorSystem,
    unmarshaller: ByteUnmarshaller[T]
) extends PubSubSubscriber[T] {
  lazy val mediator = DistributedPubSub(system).mediator

  val consumer = {
    val queueNamePrefix = (exchange.name, Utils.dockerContainerID) match {
      case ("", dockerId)           => dockerId
      case (exchangeName, "")       => exchangeName
      case (exchangeName, dockerId) => s"$exchangeName-$dockerId"
    }

    for {
      queue <- exchange.channel.queueDeclare(queueNamePrefix, randomizeName = true, durable = false, autoDelete = true)
      _     <- queue.bindTo(exchange, FanOut)
      consumer <- queue.consume { delivery =>
                   val topic   = delivery.envelope.getRoutingKey
                   val message = Message[Array[Byte]](topic, delivery.body)

                   mediator ! Publish(topic, message)
                   mediator ! Publish(Everything.topic, message)

                   queue.ack(delivery)
                 }
    } yield consumer
  }

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription =
    Subscription(system.actorOf(Props(IntermediateCallbackActor[Array[Byte], T](topic.topic, mediator, onReceive)(unmarshaller))))

  def subscribe(topic: Topic, subscriber: ActorRef): Subscription =
    Subscription(system.actorOf(Props(IntermediateForwardActor[Array[Byte], T](topic.topic, mediator, subscriber)(unmarshaller))))

  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription =
    Subscription(system.actorOf(Props(IntermediateForwardActor[Array[Byte], U](topic.topic, mediator, subscriber)(unmarshaller.andThen(converter)))))

  def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe

  override def shutdown(): Unit = {
    consumer match {
      case Success(c) => c.unsubscribe
      case Failure(_) =>
    }

    onShutdown()
  }
}
