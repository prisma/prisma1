package cool.graph.websocket.metrics

import com.prisma.errors.BugsnagErrorReporter
import cool.graph.metrics.MetricsManager
import cool.graph.profiling.MemoryProfiler

object SubscriptionWebsocketMetrics extends MetricsManager {
  MemoryProfiler.schedule(this)

  val reporter = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  override def serviceName = "SubscriptionWebsocketService"

  val activeWsConnections              = defineGauge("activeWsConnections")
  val incomingWebsocketMessageRate     = defineCounter("incomingWebsocketMessageRate")
  val outgoingWebsocketMessageRate     = defineCounter("outgoingWebsocketMessageRate")
  val incomingResponseQueueMessageRate = defineCounter("incomingResponseQueueMessageRate")
}
