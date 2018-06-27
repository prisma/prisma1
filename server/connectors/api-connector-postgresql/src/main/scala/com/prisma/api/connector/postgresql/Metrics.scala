package com.prisma.api.connector.postgresql

import com.prisma.metrics.{CustomTag, MetricsManager}

object Metrics extends MetricsManager {
  val sqlQueryTimer = defineTimer("sql.query.time", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))
}
