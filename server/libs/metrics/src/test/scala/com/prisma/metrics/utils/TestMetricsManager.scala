package com.prisma.metrics.utils

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.MetricsManager
import com.timgroup.statsd.StatsDClient
import com.prisma.metrics.DummyStatsDClient

class TestMetricsManager extends MetricsManager {
  def serviceName: String = "TestService"

  implicit val reporter = BugsnagErrorReporter("")

  override val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient   = new DummyStatsDClient
}
