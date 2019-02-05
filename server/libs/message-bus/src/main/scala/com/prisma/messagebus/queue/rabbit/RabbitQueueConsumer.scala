package com.prisma.messagebus.queue.rabbit

import akka.actor.ActorSystem
import com.prisma.errors.ErrorReporter
import com.prisma.messagebus.Conversions.ByteUnmarshaller
import com.prisma.messagebus.QueueConsumer
import com.prisma.messagebus.QueueConsumer.ConsumeFn
import com.prisma.messagebus.queue.BackoffStrategy
import com.prisma.messagebus.queue.rabbit.RabbitQueueConsumer.ProcessingFailedError
import com.prisma.rabbit.Bindings.RoutingKey
import com.prisma.rabbit.Import.Queue
import com.prisma.rabbit.{Consumer, Delivery, Exchange}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object RabbitQueueConsumer {
  // Max tries before a message is deemed failed, dropped, and requeued into the error queue
  val MAX_TRIES = 5

  // Max duration to wait in process, anything else will be requeued
  val MAX_DURATION = 60.seconds

  case class ProcessingFailedError(reason: String) extends Exception(reason)
}

/**
  * A rabbit queue consumer processes messages of type T from a specified queue using the provided onMsg function.
  *
  * The consumer will automatically retry messages exactly MAX_TRIES on failure using the backoff:
  * - If a message has exceeded the max tries, it will be queued into the error queue for inspection.
  * - The given backoff strategy decides on the duration the consumer backs off:
  *   - If the duration exceeds the MAX_DURATION threshold, it will be requeued instead with a UNIX timestamp appended that roughly designates the next processing.
  *   - Else, the processor will wait for the duration ("in process") and then start processing.
  *
  * There is a fixed number of consumers working off the given queue at any time.
  *
  * The concurrency param controls how many rabbit consumers will be created per registration of a consumer function with 'withConsumer'.
  */
case class RabbitQueueConsumer[T](
    queueName: String,
    exchange: Exchange,
    backoff: BackoffStrategy,
    concurrency: Int,
    onShutdown: () => Unit = () => {}
)(
    implicit val reporter: ErrorReporter,
    unmarshaller: ByteUnmarshaller[T],
    system: ActorSystem
) extends QueueConsumer[T] {

  val consumers: ArrayBuffer[Consumer] = ArrayBuffer[Consumer]()

  private val queues = (for {
    queue <- exchange.channel.queueDeclare(queueName, durable = false, autoDelete = true)
    errQ  <- exchange.channel.queueDeclare(s"$queueName-error", durable = false, autoDelete = false) // declare err queue to make sure error msgs are available
    _     <- errQ.bindTo(exchange, RoutingKey("error.#"))
    _     <- queue.bindTo(exchange, RoutingKey("msg.#"))
  } yield RabbitQueuesRef(queue, errQ)) match {
    case Success(qs) => qs
    case Failure(e)  => sys.error(s"Unable to declare queues: $e")
  }

  override def withConsumer(fn: ConsumeFn[T]): RabbitConsumerRef = {
    val consumer = queues.mainQ.consume(concurrency) { delivery =>
      consume(delivery, fn)
    } match {
      case Success(c) => c
      case Failure(e) => sys.error(s"Unable to declare consumer: $e")
    }

    synchronized {
      consumers ++= consumer
    }

    RabbitConsumerRef(consumer)
  }

  private def consume(delivery: Delivery, fn: ConsumeFn[T]): Unit = {
    val regularQ = queues.mainQ
    val info     = parseRoutingKey(regularQ, delivery)
    val payload  = parsePayload(regularQ, delivery)

    if (info.exceededTries) {
      val now = System.currentTimeMillis() / 1000
      exchange.publish(s"error.${info.tries}.$now", delivery.body)
      regularQ.ack(delivery)
    } else if (info.isDelayed) {
      regularQ.nack(delivery, requeue = true)
    } else {
      process(regularQ, delivery, fn, info, payload)
    }
  }

  def parsePayload(queue: Queue, delivery: Delivery): T = {
    Try { unmarshaller(delivery.body) } match {
      case Success(parsedPayload) =>
        parsedPayload

      case Failure(err) =>
        println(s"[Queue] Discarding message, invalid message body: $err")
        queue.ack(delivery)
        throw err
    }
  }

  def parseRoutingKey(queue: Queue, delivery: Delivery): MessageInfo = {
    val rk         = delivery.envelope.getRoutingKey
    val components = rk.split("\\.")

    if (components.head != "msg" || components.length < 1 || components.length > 3) {
      println(s"[Queue] Discarding message, invalid routing key: $rk")
      queue.ack(delivery)
      throw new Exception(s"Invalid routing key: $rk")
    }

    val timestamp = if (components.length == 3) {
      Some(components.last.toLong)
    } else {
      None
    }

    val tries = components(1)
    MessageInfo(tries.toInt, timestamp)
  }

  private def process(queue: Queue, delivery: Delivery, fn: T => Future[_], info: MessageInfo, payload: T): Unit = {
    val backoffDuration = BackoffStrategy.backoffDurationFor(info.tries, backoff)

    if (backoffDuration >= 60.seconds) {
      val now       = System.currentTimeMillis() / 1000
      val processAt = now + backoffDuration.toSeconds

      queue.ack(delivery)
      exchange.publish(s"msg.${info.tries}.$processAt", delivery.body)
    } else {
      val processingResult = BackoffStrategy.backoff(backoffDuration).flatMap(_ => fn(payload))

      processingResult.onComplete({
        case Success(_) =>
          queue.ack(delivery)

        case Failure(err) =>
          queue.ack(delivery)
          exchange.publish(s"msg.${info.tries + 1}", delivery.body)
          reporter.report(ProcessingFailedError(s"Processing in queue '${queue.name}' (payload '$payload') failed with error $err"))
          println(err)
      })
    }
  }

  override def shutdown: Unit = {
    println(s"[Queue] Stopping consumers for $queueName...")
    consumers.foreach { c =>
      c.unsubscribe.getOrElse(println(s"Warn: Unable to unbind consumer: $c"))
    }

    println(s"[Queue] Stopping consumers for $queueName... Done.")
    onShutdown()
  }
}
