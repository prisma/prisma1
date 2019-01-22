package com.prisma.api.connector.jdbc

import com.prisma.metrics.{CustomTag, MetricsFacade, MetricsRegistry}

object JdbcApiMetrics extends MetricsFacade {
  def init(metricsRegistry: MetricsRegistry): Unit = registry = metricsRegistry

  lazy val sqlQueryTimer = defineTimer("sql.query.time", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))
}
