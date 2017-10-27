package cool.graph.messagebus

import akka.actor.ActorRef
import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.pubsub._
import cool.graph.messagebus.queue.{BackoffStrategy, MappingQueueConsumer, MappingQueuePublisher}

import scala.concurrent.Future

/**
  * A PubSub system allows subscribers to subscribe to messages of type T and publishers to publish messages of type T.
  * Routing of messages to interested subscribers is done by topics, which route messages to subscribers.
  *
  * The original topic (routing key) is retained in the Message[T].
  *
  * Subscribers can choose to just invoke a callback on message receive, or choose to pass their own actors that allow
  * for more complex message processing scenarios. The actor must be able to handle messages of type Message[T].
  */
trait PubSub[T] extends PubSubPublisher[T] with PubSubSubscriber[T]

trait PubSubPublisher[T] extends Stoppable {
  def publish(topic: Only, msg: T): Unit
  def map[U](converter: Converter[U, T]): PubSubPublisher[U] = MappingPubSubPublisher(this, converter)
}

trait PubSubSubscriber[T] extends Stoppable {
  def subscribe(topic: Topic, onReceive: Message[T] => Unit): Subscription
  def subscribe(topic: Topic, subscriber: ActorRef): Subscription
  def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]): Subscription

  def unsubscribe(subscription: Subscription): Unit

  def map[U](converter: Converter[T, U]): PubSubSubscriber[U] = MappingPubSubSubscriber(this, converter)
}

/**
  * Queue encapsulates the consumer-producer pattern, where publishers publish work items of type T
  * and consumers work off items of type T with the given processing function.
  *
  * In case an error is encountered, a backoff strategy is utilized. The consumer waits _at least_ the specified amount
  * of time before retrying, depending on the underlying implementation details. Hence, the backoff is not a precise tool,
  * but a useful approximation. As soon as the tries are exceeded, the message is removed from regular message processing.
  *
  * The implementation can decide if it wants to discard the erroneous message, or store it somewhere for inspection/debugging.
  * Hence, this interface does not guarantee retention of erroneous messages.
  */
trait Queue[T] extends QueuePublisher[T] with QueueConsumer[T]

trait QueuePublisher[T] extends Stoppable {
  def publish(msg: T): Unit
  def map[U](converter: Converter[U, T]): QueuePublisher[U] = MappingQueuePublisher(this, converter)
}

trait QueueConsumer[T] extends Stoppable {
  val backoff: BackoffStrategy

  def withConsumer(fn: ConsumeFn[T]): ConsumerRef
  def map[U](converter: Converter[T, U]): QueueConsumer[U] = MappingQueueConsumer(this, converter)
}

object QueueConsumer {
  type ConsumeFn[T] = T => Future[_]
}

trait ConsumerRef {
  def stop: Unit
}

sealed trait Stoppable {
  def shutdown: Unit = {}
}
