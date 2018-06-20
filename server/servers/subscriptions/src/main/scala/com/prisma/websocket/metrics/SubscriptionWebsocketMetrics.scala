package com.prisma.websocket.metrics

import com.prisma.metrics.MetricsManager

object SubscriptionWebsocketMetrics extends MetricsManager {
  val activeWsConnections               = defineGauge("websocket.connections.gauge")
  val incomingWebsocketMessageCount     = defineCounter("websocket.messages.incoming.count")
  val outgoingWebsocketMessageCount     = defineCounter("websocket.messages.outgoing.count")
  val incomingResponseQueueMessageCount = defineCounter("websocket.responseQueue.count")
}
