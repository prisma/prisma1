package cool.graph.subscriptions.metrics

import com.prisma.errors.BugsnagErrorReporter
import cool.graph.metrics.{CustomTag, MetricsManager}
import cool.graph.profiling.MemoryProfiler

object SubscriptionMetrics extends MetricsManager {
  override def serviceName = "SimpleSubscriptionService"

  val reporter = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  MemoryProfiler.schedule(this)

  // Actor Counts
  val activeSubcriptionSessions                     = defineGauge("activeSubscriptionSessions")
  val activeSubscriptionsManagerForProject          = defineGauge("activeSubscriptionsManagerForProject")
  val activeSubscriptionsManagerForModelAndMutation = defineGauge("activeSubscriptionsManagerForModelAndMutation")
  val activeSubscriptions                           = defineGauge("activeSubscriptions")

  val projectIdTag             = CustomTag("projectId")
  val databaseEventRate        = defineCounter("databaseEventRate", projectIdTag)
  val handleDatabaseEventRate  = defineCounter("handleDatabaseEventRate", projectIdTag)
  val handleDatabaseEventTimer = defineTimer("databaseEventTimer", projectIdTag)
}
