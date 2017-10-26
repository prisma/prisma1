package cool.graph.metrics.utils

import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import cool.graph.metrics.MetricsManager

class TestLiveMetricsManager extends MetricsManager {
  def serviceName: String = "TestService"

  override val baseTagsString: String = "env=test,instance=local,container=none"
  override val client: StatsDClient   = new NonBlockingStatsDClient(serviceName, "127.0.0.1", 8125, new Array[String](0), errorHandler)
}
