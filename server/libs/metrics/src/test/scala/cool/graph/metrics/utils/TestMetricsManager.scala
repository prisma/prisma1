package cool.graph.metrics.utils

import com.prisma.errors.BugsnagErrorReporter
import com.timgroup.statsd.StatsDClient
import cool.graph.metrics.{DummyStatsDClient, MetricsManager}

class TestMetricsManager extends MetricsManager {
  def serviceName: String = "TestService"

  implicit val reporter = BugsnagErrorReporter("")

  override val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient   = new DummyStatsDClient
}
