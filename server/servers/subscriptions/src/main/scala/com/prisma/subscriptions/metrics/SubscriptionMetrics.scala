package com.prisma.subscriptions.metrics

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.{CustomTag, MetricsManager}
import com.prisma.profiling.JvmProfiler

object SubscriptionMetrics extends MetricsManager(BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))) {
  override def serviceName = "Subscriptions"

  JvmProfiler.schedule(this)

  // Actor Counts
  val activeSubcriptionSessions            = defineGauge("subscriptions.sessions")
  val activeSubscriptionsManagerForProject = defineGauge("subscriptions.manager.project")
  val activeSubscriptionsManagerForModel   = defineGauge("subscriptions.manager.model")
  val activeSubscriptions                  = defineGauge("subscriptions.active")

  val projectIdTag             = CustomTag("projectId")
  val databaseEventRate        = defineCounter("subscriptions.databaseEvent.count", projectIdTag)
  val handleDatabaseEventRate  = defineCounter("subscriptions.handleDatabaseEvent.count", projectIdTag)
  val handleDatabaseEventTimer = defineTimer("subscriptions.databaseEvent.time", projectIdTag)
}
