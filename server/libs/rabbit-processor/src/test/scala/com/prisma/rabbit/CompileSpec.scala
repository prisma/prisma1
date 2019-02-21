package com.prisma.rabbit

import com.prisma.errors.DummyErrorReporter
import com.prisma.rabbit.Bindings.FanOut

import scala.util.{Failure, Success, Try}

object CompileSpec {
  implicit val reporter = DummyErrorReporter
  val amqpUri           = "amqp://localhost"
  val queueName         = "some-name"

  // Consume with 1 consumer
  for {
    channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
    queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
    exchange <- channel.exchangeDeclare("some-exchange", durable = false)
    _        <- queue.bindTo(exchange, FanOut)
    _ <- queue.consume { delivery =>
          // do something with the delivery and ack afterwards
          println(delivery.body)
          queue.ack(delivery)
        }
  } yield ()

  // Consume with multiple consumers
  val numberOfConsumers = 4
  for {
    channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = numberOfConsumers)
    queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
    exchange <- channel.exchangeDeclare("some-exchange", durable = false)
    _        <- queue.bindTo(exchange, Bindings.FanOut)
    _ <- queue.consume(numberOfConsumers) { delivery =>
          // do something with the delivery and ack afterwards
          println(delivery.body)
          queue.ack(delivery)
        }
  } yield ()

  // Publishing
  def setupMyCustomExchange: Exchange = {
    val exchange: Try[Exchange] = for {
      channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
      exchange <- channel.exchangeDeclare("some-exchange", durable = false)
    } yield exchange
    exchange match {
      case Success(x) =>
        x
      case Failure(e) =>
        // maybe do something to retry. A naive way could look like this:
        Thread.sleep(1000)
        setupMyCustomExchange
    }
  }
  val exchange = setupMyCustomExchange
  exchange.publish("routingKey", "some message")

  // Publish to a Queue
  def setupMyQueue: Queue = {
    val queue: Try[Queue] = for {
      channel <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
      queue   <- channel.queueDeclare("my-queue", durable = false, autoDelete = true)
    } yield queue
    queue match {
      case Success(x) =>
        x
      case Failure(e) =>
        // maybe do something to retry. A naive way could look like this:
        Thread.sleep(1000)
        setupMyQueue
    }
  }
  val queue = setupMyQueue
  queue.publish("some message")
}
