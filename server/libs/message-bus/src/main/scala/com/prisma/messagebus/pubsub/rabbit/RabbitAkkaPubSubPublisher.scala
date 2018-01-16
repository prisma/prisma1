package cool.graph.messagebus.pubsub.rabbit

import cool.graph.messagebus.Conversions.ByteMarshaller
import cool.graph.messagebus.pubsub.Only
import cool.graph.messagebus.PubSubPublisher
import cool.graph.rabbit.Import.Exchange

case class RabbitAkkaPubSubPublisher[T](
    exchange: Exchange,
    onShutdown: () => Unit = () => ()
)(implicit marshaller: ByteMarshaller[T])
    extends PubSubPublisher[T] {
  def publish(topic: Only, msg: T): Unit = exchange.publish(topic.topic, marshaller(msg))
  override def shutdown: Unit            = onShutdown()
}
