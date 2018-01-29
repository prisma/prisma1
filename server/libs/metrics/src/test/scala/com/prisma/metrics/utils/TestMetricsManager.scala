package com.prisma.metrics.utils

import com.prisma.errors.BugsnagErrorReporter
import com.timgroup.statsd.StatsDClient
import com.prisma.metrics.{DummyStatsDClient, MetricsManager}

class TestMetricsManager extends MetricsManager(BugsnagErrorReporter("")) {
  def serviceName: String = "TestService"

  override val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient   = new DummyStatsDClient
}
