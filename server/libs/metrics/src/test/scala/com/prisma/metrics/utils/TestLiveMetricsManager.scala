package com.prisma.metrics.utils

import com.prisma.errors.BugsnagErrorReporter
import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import com.prisma.metrics.MetricsManager

class TestLiveMetricsManager extends MetricsManager(BugsnagErrorReporter("")) {
  def serviceName: String = "TestService"

  override lazy val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient        = new NonBlockingStatsDClient(serviceName, "127.0.0.1", 8125, new Array[String](0), errorHandler)
}
