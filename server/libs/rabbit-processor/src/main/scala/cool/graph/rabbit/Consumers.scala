package cool.graph.rabbit

import com.rabbitmq.client.{AMQP, DefaultConsumer, Envelope, Channel => RabbitChannel}
import cool.graph.bugsnag.{BugSnagger, MetaData}

import scala.util.{Failure, Try}

case class DeliveryConsumer(channel: Channel, f: Delivery => Unit)(implicit bugsnagger: BugSnagger) extends DefaultConsumer(channel.rabbitChannel) {

  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
    val delivery = Delivery(body, envelope, properties)
    Try {
      f(delivery)
    } match {
      case Failure(e) =>
        val bodyAsString = Try(new String(body)).getOrElse("Message Bytes could not be converted into a String.")
        val metaData     = Seq(MetaData("Rabbit", "messageBody", bodyAsString))
        bugsnagger.report(e, metaData)
      case _ => {} // NO-OP
    }
  }
}
