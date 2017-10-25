package cool.graph.rabbit

import java.nio.charset.StandardCharsets

import com.rabbitmq.client.{Channel => RabbitChannel, Consumer => RabbitConsumer, _}
import cool.graph.bugsnag.BugSnagger
import cool.graph.rabbit.Bindings.Binding
import cool.graph.rabbit.ExchangeTypes._

import scala.util.Try

object Rabbit {
  def channel(name: String, amqpUri: String, consumerThreads: Int)(implicit bugSnag: BugSnagger): Try[Channel] = {
    channel(name, amqpUri, consumerThreads, None)
  }

  def channel(name: String, amqpUri: String, consumerThreads: Int, qos: Int)(implicit bugSnag: BugSnagger): Try[Channel] = {
    channel(name, amqpUri, consumerThreads, Some(qos))
  }

  def channel(name: String, amqpUri: String, consumerThreads: Int, qos: Option[Int])(implicit bugSnag: BugSnagger): Try[Channel] = {
    PlainRabbit.connect(name, amqpUri, consumerThreads, qos).map { channel =>
      Channel(channel)
    }
  }
}

case class Channel(rabbitChannel: RabbitChannel) {
  def queueDeclare(name: String, randomizeName: Boolean = false, durable: Boolean, autoDelete: Boolean): Try[Queue] =
    Try {
      val exclusive = false
      val queueName = if (randomizeName) {
        s"$name-" + Utils.timestampWithRandom
      } else {
        name
      }
      rabbitChannel.queueDeclare(queueName, durable, exclusive, autoDelete, null)
      Queue(queueName, this)
    }

  def exchangeDeclare(name: String, durable: Boolean, autoDelete: Boolean = false, confirm: Boolean = false): Try[Exchange] = Try {
    import collection.JavaConversions.mapAsJavaMap
    val internal = false
    rabbitChannel
      .exchangeDeclare(name, BuiltinExchangeType.TOPIC, durable, autoDelete, mapAsJavaMap(Map.empty[String, Object]))
    if (confirm) {
      rabbitChannel.confirmSelect()
    }
    Exchange(name, this)
  }

  def close(alsoCloseConnection: Boolean = true): Try[Unit] = Try {
    rabbitChannel.close
    if (alsoCloseConnection) {
      rabbitChannel.getConnection.close
    }
  }
}

case class Queue(name: String, channel: Channel) {
  val rabbitChannel: RabbitChannel = channel.rabbitChannel

  def bindTo(exchange: Exchange, binding: Binding): Try[Unit] = bindTo(exchange.name, binding)

  def bindTo(exchangeName: String, binding: Binding): Try[Unit] = Try {
    rabbitChannel.queueBind(name, exchangeName, binding.routingKey)
  }

  def consume(f: Delivery => Unit)(implicit bugSnag: BugSnagger): Try[Consumer] = consume(1)(f).map(_.head)

  def consume(numberOfConsumers: Int = 1)(f: Delivery => Unit)(implicit bugSnag: BugSnagger): Try[Seq[Consumer]] =
    Try {
      (1 to numberOfConsumers).map { _ =>
        consume(DeliveryConsumer(channel, f)).get // get the result so we get the exception if something fails
      }
    }
  def consume(consumer: RabbitConsumer): Try[Consumer] = Try {
    val autoAck     = false
    val consumerTag = s"$name-queue-" + Utils.timestampWithRandom
    rabbitChannel.basicConsume(name, autoAck, consumerTag, consumer)
    Consumer(consumerTag, channel)
  }

  def publish(body: String): Unit      = publish(body.getBytes)
  def publish(body: Array[Byte]): Unit = rabbitChannel.basicPublish("", name, null, body)

  def ack(delivery: Delivery): Unit = ack(delivery.envelope.getDeliveryTag)
  def ack(deliveryTag: Long): Unit = {
    val multiple = false
    rabbitChannel.basicAck(deliveryTag, multiple)
  }

  def nack(delivery: Delivery, requeue: Boolean): Unit = nack(delivery.envelope.getDeliveryTag, requeue)
  def nack(deliveryTag: Long, requeue: Boolean): Unit = {
    val multiple = false
    rabbitChannel.basicNack(deliveryTag, multiple, requeue)
  }
}
case class Delivery(body: Array[Byte], envelope: Envelope, properties: AMQP.BasicProperties) {
  lazy val bodyAsString: String = new String(body, StandardCharsets.UTF_8)
}
case class Consumer(consumerTag: String, channel: Channel) {
  val rabbitChannel = channel.rabbitChannel

  def unsubscribe: Try[Unit] = Try {
    rabbitChannel.basicCancel(consumerTag)
  }
}

case class Exchange(name: String, channel: Channel) {
  val rabbitChannel = channel.rabbitChannel

  def publish(routingKey: String, body: String): Unit = {
    val bytes: Array[Byte] = body.getBytes
    this.publish(routingKey, bytes)
  }

  def publish(routingKey: String, body: Array[Byte]): Unit = {
    rabbitChannel.basicPublish(name, routingKey, null, body)
  }
}

object ExchangeTypes {
  sealed trait ExchangeType {
    def rabbitTypeString: String
  }
  object Topic extends ExchangeType {
    def rabbitTypeString = "topic"
  }
}

object Bindings {
  // "The binding key must also be in the same form. The logic behind the topic exchange is similar to a direct one - a message sent with a particular routing key will be delivered to all the queues that are bound with a matching binding key."
  // * (star) can substitute for exactly one word.
  // # (hash) can substitute for zero or more words.
  sealed trait Binding {
    def routingKey: String
  }
  case class RoutingKey(routingKey: String) extends Binding
  case object FanOut extends Binding {
    override def routingKey = "#"
  }
}
