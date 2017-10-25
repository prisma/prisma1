package cool.graph.messagebus.queue.rabbit

import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.ByteUnmarshaller
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.BackoffStrategy
import cool.graph.messagebus.{ConsumerRef, QueueConsumer}
import cool.graph.rabbit.Bindings.RoutingKey
import cool.graph.rabbit.Import.Queue
import cool.graph.rabbit.{Consumer, Delivery, Exchange}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/**
  * A plain rabbit queue consumer without magic that processes messages of type T from the specified queue.
  * This consumer is the bare minimum and doesn't do anything besides message parsing and passing it to
  * the consume function.
  *
  * It also doesn't bind the queue to any routing key if None is given, effectively assuming that the queue is already
  * bound to something if you want to consume a steady flow of messages.
  */
case class RabbitPlainQueueConsumer[T](
    queueName: String,
    exchange: Exchange,
    backoff: BackoffStrategy,
    autoDelete: Boolean = true,
    onShutdown: () => Unit = () => {},
    routingKey: Option[String] = None
)(implicit val bugSnagger: BugSnagger, unmarshaller: ByteUnmarshaller[T])
    extends QueueConsumer[T] {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val consumers = ArrayBuffer[Consumer]()

  val queue: Queue = (for {
    queue <- exchange.channel.queueDeclare(queueName, durable = false, autoDelete = autoDelete)
    _ = routingKey match {
      case Some(rk) => queue.bindTo(exchange, RoutingKey(rk))
      case _        =>
    }
  } yield queue) match {
    case Success(q) => q
    case Failure(e) => sys.error(s"Unable to declare queue: $e")
  }

  override def withConsumer(fn: ConsumeFn[T]): ConsumerRef = {
    val consumer = queue.consume { delivery =>
      val payload = parsePayload(queue, delivery)
      fn(payload).onComplete {
        case Success(_)   => queue.ack(delivery)
        case Failure(err) => queue.nack(delivery, requeue = true); println(err)
      }
    } match {
      case Success(c) => c
      case Failure(e) => sys.error(s"Unable to declare consumer: $e")
    }

    RabbitConsumerRef(Seq(consumer))
  }

  def parsePayload(queue: Queue, delivery: Delivery): T = {
    Try { unmarshaller(delivery.body) } match {
      case Success(parsedPayload) =>
        parsedPayload

      case Failure(err) =>
        println(s"[Plain Consumer] Discarding message, invalid message body: $err")
        queue.ack(delivery)
        throw err
    }
  }

  override def shutdown: Unit = {
    println(s"[Plain Consumer] Stopping...")
    consumers.foreach { c =>
      c.unsubscribe.getOrElse(s"[Plain Consumer] Warn: Unable to unbind consumer: $c")
    }
    println(s"[Plain Consumer] Stopping... Done.")

    onShutdown()
  }
}
