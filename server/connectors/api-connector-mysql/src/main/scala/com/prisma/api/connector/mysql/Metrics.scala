package com.prisma.api.connector.mysql

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.{CustomTag, MetricsManager}

object Metrics extends MetricsManager(BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))) {
  val sqlQueryTimer = defineTimer("sql.query.time", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))

  override def serviceName = "MySqlConnector"
}
