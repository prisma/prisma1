package cool.graph.messagebus.queue

import cool.graph.messagebus.Conversions.Converter
import cool.graph.messagebus.{ConsumerRef, Queue, QueueConsumer, QueuePublisher}

import scala.concurrent.Future

/**
  * QueueConsumer decorator that allows consumers to transparently consume a different type using the given
  * converter to map the original consumer type to the newly expected type.
  */
case class MappingQueueConsumer[A, B](queueConsumer: QueueConsumer[A], converter: Converter[A, B]) extends QueueConsumer[B] {
  val backoff = queueConsumer.backoff

  override def withConsumer(fn: B => Future[_]): ConsumerRef = queueConsumer.withConsumer(theA => fn(converter(theA)))
  override def shutdown(): Unit                              = queueConsumer.shutdown
}

/**
  * QueuePublisher decorator that allows publishers to transparently publish a different type using the given
  * converter to map the original publish type to the type of the underlying publisher.
  */
case class MappingQueuePublisher[B, A](queuePublisher: QueuePublisher[A], converter: Converter[B, A]) extends QueuePublisher[B] {
  override def publish(msg: B): Unit = queuePublisher.publish(converter(msg))
}
