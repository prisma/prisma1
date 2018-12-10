package com.prisma.subscriptions.metrics

import com.prisma.metrics.{CustomTag, MetricsFacade, MetricsRegistry}

object SubscriptionMetrics extends MetricsFacade {
  def init(metricsRegistry: MetricsRegistry): Unit = {
    registry = metricsRegistry
  }

  // Actor Counts
  lazy val activeSubcriptionSessions            = defineGauge("subscriptions.sessions")
  lazy val activeSubscriptionsManagerForProject = defineGauge("subscriptions.manager.project")
  lazy val activeSubscriptionsManagerForModel   = defineGauge("subscriptions.manager.model")
  lazy val activeSubscriptions                  = defineGauge("subscriptions.active")

  lazy val projectIdTag             = CustomTag("projectId")
  lazy val databaseEventRate        = defineCounter("subscriptions.databaseEvent.count", projectIdTag)
  lazy val handleDatabaseEventRate  = defineCounter("subscriptions.handleDatabaseEvent.count", projectIdTag)
  lazy val handleDatabaseEventTimer = defineTimer("subscriptions.databaseEvent.time", projectIdTag)
}
