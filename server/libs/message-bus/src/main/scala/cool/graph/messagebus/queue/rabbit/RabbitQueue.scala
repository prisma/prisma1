package cool.graph.messagebus.queue.rabbit

import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller}
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.{ConsumerRef, Queue}
import cool.graph.messagebus.queue.{BackoffStrategy, LinearBackoff}
import cool.graph.messagebus.utils.RabbitUtils
import cool.graph.rabbit.Consumer
import cool.graph.rabbit.Import.{Exchange, Queue => RMQueue}

import scala.concurrent.Future
import scala.concurrent.duration._

case class RabbitQueue[T](
    amqpUri: String,
    exchangeName: String,
    backoff: BackoffStrategy,
    durableExchange: Boolean = false,
    exchangeConcurrency: Int = 1,
    workerConcurrency: Int = 1
)(
    implicit bugSnagger: BugSnagger,
    marshaller: ByteMarshaller[T],
    unmarshaller: ByteUnmarshaller[T]
) extends Queue[T] {

  val exchange: Exchange                 = RabbitUtils.declareExchange(amqpUri, exchangeName, exchangeConcurrency, durableExchange)
  val publisher: RabbitQueuePublisher[T] = RabbitQueuePublisher[T](exchange)
  val consumer: RabbitQueueConsumer[T]   = RabbitQueueConsumer[T](exchangeName, exchange, backoff, workerConcurrency)

  def publish(msg: T): Unit = publisher.publish(msg)

  override def shutdown: Unit = {
    consumer.shutdown
    publisher.shutdown
    exchange.channel.close()
  }

  override def withConsumer(fn: ConsumeFn[T]): RabbitConsumerRef = consumer.withConsumer(fn)
}

case class RabbitConsumerRef(consumers: Seq[Consumer]) extends ConsumerRef {
  override def stop: Unit = consumers.foreach { consumer =>
    consumer.unsubscribe.getOrElse(println(s"Warn: Unable to unbind consumer $consumer"))
  }
}

case class RabbitQueuesRef(mainQ: RMQueue, errorQ: RMQueue)

/**
  * Collection of convenience standalone initializers for Rabbit-based queueing
  */
object RabbitQueue {

  def publisher[T](
      amqpUri: String,
      exchangeName: String,
      concurrency: Int = 1,
      durable: Boolean = false
  )(implicit bugSnagger: BugSnagger, marshaller: ByteMarshaller[T]): RabbitQueuePublisher[T] = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, concurrency, durable)

    RabbitQueuePublisher[T](exchange, onShutdown = () => {
      Future.fromTry(exchange.channel.close())
    })
  }

  def consumer[T](
      amqpUri: String,
      exchangeName: String,
      exchangeConcurrency: Int = 1,
      workerConcurrency: Int = 1,
      durableExchange: Boolean = false,
      backoff: BackoffStrategy = LinearBackoff(5.seconds)
  )(implicit bugSnagger: BugSnagger, unmarshaller: ByteUnmarshaller[T]): RabbitQueueConsumer[T] = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, exchangeConcurrency, durableExchange)

    RabbitQueueConsumer[T](exchangeName, exchange, backoff, workerConcurrency, onShutdown = () => exchange.channel.close())
  }

  def plainConsumer[T](
      amqpUri: String,
      queueName: String,
      exchangeName: String,
      exchangeConcurrency: Int = 1,
      workerConcurrency: Int = 1,
      autoDelete: Boolean = true,
      durableExchange: Boolean = false,
      backoff: BackoffStrategy = LinearBackoff(5.seconds)
  )(implicit bugSnagger: BugSnagger, unmarshaller: ByteUnmarshaller[T]): RabbitPlainQueueConsumer[T] = {
    val exchange = RabbitUtils.declareExchange(amqpUri, exchangeName, exchangeConcurrency, durableExchange)

    RabbitPlainQueueConsumer[T](queueName, exchange, backoff, autoDelete = autoDelete, onShutdown = () => exchange.channel.close())
  }
}
