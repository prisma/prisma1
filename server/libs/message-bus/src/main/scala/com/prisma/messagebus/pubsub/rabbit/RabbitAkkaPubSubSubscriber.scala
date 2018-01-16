package com.prisma.messagebus.pubsub.rabbit

import akka.actor.{ActorRef, ActorSystem, Props}
import com.prisma.errors.ErrorReporter
import com.prisma.messagebus.Conversions.{ByteUnmarshaller, Converter}
import com.prisma.messagebus._
import com.prisma.messagebus.pubsub.PubSubProtocol.Publish
import com.prisma.messagebus.pubsub._
import com.prisma.messagebus.utils.Utils
import com.prisma.rabbit.Bindings.FanOut
import com.prisma.rabbit.Import.Exchange

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
    implicit val reporter: ErrorReporter,
    val system: ActorSystem,
    unmarshaller: ByteUnmarshaller[T]
) extends PubSubSubscriber[T] {
  lazy val router = system.actorOf(Props(PubSubRouter()))

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

                   router ! Publish(topic, message)
                   router ! Publish(Everything.topic, message)

                   queue.ack(delivery)
                 }
    } yield consumer
  }

  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription =
    Subscription(topic.topic, system.actorOf(Props(IntermediateCallbackActor[Array[Byte], T](topic.topic, router, onReceive)(unmarshaller))))

  def subscribe(topic: Topic, subscriber: ActorRef): Subscription =
    Subscription(topic.topic, system.actorOf(Props(IntermediateForwardActor[Array[Byte], T](topic.topic, router, subscriber)(unmarshaller))))

  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription =
    Subscription(
      topic.topic,
      system.actorOf(Props(IntermediateForwardActor[Array[Byte], U](topic.topic, router, subscriber)(unmarshaller.andThen(converter))))
    )

  def unsubscribe(subscription: Subscription): Unit = subscription.unsubscribe

  override def shutdown(): Unit = {
    consumer match {
      case Success(c) => c.unsubscribe
      case Failure(_) =>
    }

    system.stop(router)
    onShutdown()
  }
}
