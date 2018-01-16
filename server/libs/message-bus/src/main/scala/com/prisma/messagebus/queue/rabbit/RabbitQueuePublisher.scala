package cool.graph.messagebus.queue.rabbit

import cool.graph.messagebus.Conversions.ByteMarshaller
import cool.graph.messagebus.QueuePublisher
import cool.graph.rabbit.Exchange

import scala.concurrent.Future

/**
  * Publishing messages follows a specific pattern: "[prefix].[tries].[next processing timestamp, optional]"
  * The [prefix] decides where the message will be routed. There are 2 queues:
  *   - The main workoff queue, prefixed with "msg"
  *   - The error queue, where messages will be routed that have the "error" prefix. The consumer implementation will
  *     route messages that failed fatally or exceeded the max retries to the error queue.
  *
  * The [tries] designates how many retries a message already had, starting at 0.
  *
  * The [next processing timestamp] is used when a message has to back off for a certain amount of time. If the consumer
  * implementation sees this timestamp, it will just requeue the message if the timestamp is in the future.
  */
case class RabbitQueuePublisher[T](exchange: Exchange, onShutdown: () => Unit = () => {})(implicit val marshaller: ByteMarshaller[T])
    extends QueuePublisher[T] {

  def publish(msg: T): Unit   = exchange.publish(s"msg.0", marshaller(msg))
  override def shutdown: Unit = onShutdown()
}
