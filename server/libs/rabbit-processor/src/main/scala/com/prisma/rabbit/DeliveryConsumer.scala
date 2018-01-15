package com.prisma.rabbit

import com.prisma.errors.{ErrorReporter, GenericMetadata}
import com.rabbitmq.client.{AMQP, DefaultConsumer, Envelope, Channel => RabbitChannel}

import scala.util.{Failure, Try}

case class DeliveryConsumer(channel: Channel, f: Delivery => Unit)(implicit reporter: ErrorReporter) extends DefaultConsumer(channel.rabbitChannel) {

  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
    val delivery = Delivery(body, envelope, properties)
    Try {
      f(delivery)
    } match {
      case Failure(e) =>
        val bodyAsString = Try(new String(body)).getOrElse("Message Bytes could not be converted into a String.")
        reporter.report(e, GenericMetadata("Rabbit", "MessageBody", bodyAsString))

      case _ =>
      // NO-OP
    }
  }
}
