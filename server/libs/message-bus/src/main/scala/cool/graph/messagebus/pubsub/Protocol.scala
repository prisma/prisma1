package cool.graph.messagebus.pubsub

import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe

/**
  * A topic describes the messages a subscriber is interested in or a publisher wants to publish to.
  *
  * Only messages matching the topic will be received by a subscribing actor or will invoke a given callback.
  */
sealed trait Topic {
  val topic: String
}

/**
  * Topic describing the "everything" topic, where all messages are published in addition to their regular topic.
  * E.g. each message read from rabbit with topic "X" is published to all subscribers of "X" and additionally to the
  * special topic "everything".
  *
  * Messages should never be published to "everything" manually.
  */
object Everything extends Topic {
  override val topic: String = "everything"
}

/**
  * Topic to subscribe or publish to.
  */
case class Only(topic: String) extends Topic

/**
  * Subscription describes a specific subscription of one actor or callback to one topic.
  *
  * @param intermediatePubSubActor The intermediate actor used to parse messages and forward messages to an actor or
  *                                invoke callbacks.
  */
case class Subscription(intermediatePubSubActor: ActorRef) {
  def unsubscribe: Unit = intermediatePubSubActor ! Unsubscribe
}

/**
  * Represents a received message in pub sub. This is what is send to the subscriber (actor or callback fn).
  *
  * @param topic The original topic with which the message arrived. (i.e. can't be )
  * @param payload The message payload
  */
case class Message[T](topic: String, payload: T) {
  def map[U](fn: T => U): Message[U] = Message(topic, fn(payload))
}
