package cool.graph.client.metrics

import cool.graph.metrics.MetricsManager

object BackendSharedMetrics extends MetricsManager {

  // CamelCase the service name read from env
  override def serviceName =
    sys.env
      .getOrElse("SERVICE_NAME", "BackendShared")
      .split("-")
      .map { x =>
        x.head.toUpper + x.tail
      }
      .mkString

  val projectCacheGetCount  = defineCounter("projectCacheGetCount")
  val projectCacheMissCount = defineCounter("projectCacheMissCount")
}
