package com.prisma.metrics.utils

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.{DummyStatsDClient, MetricsManager}
import com.timgroup.statsd.StatsDClient

class TestMetricsManager extends MetricsManager(BugsnagErrorReporter("")) {
  def serviceName: String = "TestService"

  override lazy val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient        = new DummyStatsDClient
}
