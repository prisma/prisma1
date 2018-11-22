package com.prisma.api.connector.jdbc

import com.prisma.metrics.{CustomTag, MetricsFacade}

object Metrics extends MetricsFacade {
  val sqlQueryTimer = defineTimer("sql.query.time", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))
}
