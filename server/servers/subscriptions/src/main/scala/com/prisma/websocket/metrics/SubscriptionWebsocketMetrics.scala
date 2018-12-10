package com.prisma.websocket.metrics

import com.prisma.metrics.{MetricsFacade, MetricsRegistry}

object SubscriptionWebsocketMetrics extends MetricsFacade {
  def init(metricsRegistry: MetricsRegistry): Unit = registry = metricsRegistry

  lazy val activeWsConnections               = defineGauge("websocket.connections.gauge")
  lazy val incomingWebsocketMessageCount     = defineCounter("websocket.messages.incoming.count")
  lazy val outgoingWebsocketMessageCount     = defineCounter("websocket.messages.outgoing.count")
  lazy val incomingResponseQueueMessageCount = defineCounter("websocket.responseQueue.count")
}
