package com.prisma.websocket.metrics

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.MetricsManager
import com.prisma.profiling.JvmProfiler

object SubscriptionWebsocketMetrics extends MetricsManager(BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))) {
  JvmProfiler.schedule(this)

  override def serviceName = "Websockets"

  val activeWsConnections               = defineGauge("websocket.connections.gauge")
  val incomingWebsocketMessageCount     = defineCounter("websocket.messages.incoming.count")
  val outgoingWebsocketMessageCount     = defineCounter("websocket.messages.outgoing.count")
  val incomingResponseQueueMessageCount = defineCounter("websocket.responseQueue.count")
}
