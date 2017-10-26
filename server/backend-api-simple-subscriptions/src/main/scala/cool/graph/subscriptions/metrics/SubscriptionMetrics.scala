package cool.graph.subscriptions.metrics

import cool.graph.metrics.{CustomTag, MetricsManager}
import cool.graph.profiling.MemoryProfiler

object SubscriptionMetrics extends MetricsManager {
  override def serviceName = "SimpleSubscriptionService"

  MemoryProfiler.schedule(this)

  // Actor Counts
  val activeSubcriptionSessions                     = defineGauge("activeSubscriptionSessions")
  val activeSubscriptionsManagerForProject          = defineGauge("activeSubscriptionsManagerForProject")
  val activeSubscriptionsManagerForModelAndMutation = defineGauge("activeSubscriptionsManagerForModelAndMutation")

  val activeSubscriptions = defineGauge("activeSubscriptions")

  val projectIdTag             = CustomTag("projectId")
  val databaseEventRate        = defineCounter("databaseEventRate", projectIdTag)
  val handleDatabaseEventRate  = defineCounter("handleDatabaseEventRate", projectIdTag)
  val handleDatabaseEventTimer = defineTimer("databaseEventTimer", projectIdTag)
}
