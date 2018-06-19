package com.prisma.subscriptions.metrics

import com.prisma.metrics.{CustomTag, MetricsManager}

object SubscriptionMetrics extends MetricsManager {
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
