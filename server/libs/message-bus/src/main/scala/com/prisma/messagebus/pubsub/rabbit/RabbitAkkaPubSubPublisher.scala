package com.prisma.messagebus.pubsub.rabbit

import com.prisma.messagebus.Conversions.ByteMarshaller
import com.prisma.messagebus.pubsub.Only
import com.prisma.messagebus.PubSubPublisher
import com.prisma.rabbit.Import.Exchange

case class RabbitAkkaPubSubPublisher[T](
    exchange: Exchange,
    onShutdown: () => Unit = () => ()
)(implicit marshaller: ByteMarshaller[T])
    extends PubSubPublisher[T] {
  def publish(topic: Only, msg: T): Unit = exchange.publish(topic.topic, marshaller(msg))
  override def shutdown: Unit            = onShutdown()
}
