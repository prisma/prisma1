package com.prisma.messagebus.pubsub

import akka.actor.{Actor, ActorRef, Terminated}
import com.prisma.messagebus.Conversions.Converter
import com.prisma.messagebus.pubsub.PubSubProtocol.{Subscribe, Unsubscribe}

/**
  * Actor receiving all messages that are published to the specified  topic.
  * Parses the incoming message body from Array[Byte] to T using the provided unmarshaller and forwards the result as Message[T]
  * to the targetActor, which can then do the main processing.
  *
  * Terminates when the targetActor terminates or an unsubscribe is received. However, doesn NOT stop the targetActor on
  * unsubscribe.
  */
case class IntermediateForwardActor[T, U](topic: String, mediator: ActorRef, targetActor: ActorRef)(implicit converter: Converter[T, U]) extends Actor {
  context.watch(targetActor)

  mediator ! Subscribe(topic, self)

  override def receive: Receive = {
    case Message(t, msg) => targetActor ! Message(t, converter(msg.asInstanceOf[T]))
    case Terminated(_)   => context.stop(self)
    case Unsubscribe     => context.stop(self)
  }
}

/**
  * Actor receiving all messages that are published to the specified  topic.
  * Parses the message to T using the provided unmarshaller and invokes the given callback with the result as Message[T].
  *
  * Terminates when an unsubscribe is received.
  */
case class IntermediateCallbackActor[T, U](topic: String, mediator: ActorRef, callback: Message[U] => Unit)(implicit converter: Converter[T, U]) extends Actor {
  mediator ! Subscribe(topic, self)

  override def receive: Receive = {
    case Message(t, msg) =>
      callback(Message(t, converter(msg.asInstanceOf[T])))

    case Unsubscribe =>
      mediator ! Unsubscribe(topic, self)
      context.stop(self)
  }
}
