package cool.graph.rabbit

import com.rabbitmq.client.impl.DefaultExceptionHandler
import com.rabbitmq.client.{Connection, TopologyRecoveryException, Channel => RabbitChannel, Consumer => RabbitConsumer}
import cool.graph.bugsnag.BugSnagger

case class RabbitExceptionHandler(bugSnag: BugSnagger) extends DefaultExceptionHandler {

  override def handleConsumerException(channel: RabbitChannel, exception: Throwable, consumer: RabbitConsumer, consumerTag: String, methodName: String): Unit = {

    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleConsumerException", exception))
    super.handleConsumerException(channel, exception, consumer, consumerTag, methodName)
  }

  override def handleUnexpectedConnectionDriverException(conn: Connection, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleUnexpectedConnectionDriverException", exception))
    super.handleUnexpectedConnectionDriverException(conn, exception)
  }

  override def handleBlockedListenerException(connection: Connection, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleBlockedListenerException", exception))
    super.handleBlockedListenerException(connection, exception)
  }

  override def handleChannelRecoveryException(ch: RabbitChannel, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleChannelRecoveryException", exception))
    super.handleChannelRecoveryException(ch, exception)
  }

  override def handleFlowListenerException(channel: RabbitChannel, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleFlowListenerException", exception))
    super.handleFlowListenerException(channel, exception)
  }

  override def handleReturnListenerException(channel: RabbitChannel, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleReturnListenerException", exception))
    super.handleReturnListenerException(channel, exception)
  }

  override def handleTopologyRecoveryException(conn: Connection, ch: RabbitChannel, exception: TopologyRecoveryException): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleTopologyRecoveryException", exception))
    super.handleTopologyRecoveryException(conn, ch, exception)
  }

  override def handleConfirmListenerException(channel: RabbitChannel, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleConfirmListenerException", exception))
    super.handleConfirmListenerException(channel, exception)
  }

  override def handleConnectionRecoveryException(conn: Connection, exception: Throwable): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleConnectionRecoveryException", exception))
    super.handleConnectionRecoveryException(conn, exception)
  }

  override def handleChannelKiller(channel: RabbitChannel, exception: Throwable, what: String): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleChannelKiller", exception))
    super.handleChannelKiller(channel, exception, what)
  }

  override def handleConnectionKiller(connection: Connection, exception: Throwable, what: String): Unit = {
    bugSnag.report(new RuntimeException("Rabbit error occurred. -> handleConnectionKiller", exception))
    super.handleConnectionKiller(connection, exception, what)
  }
}
