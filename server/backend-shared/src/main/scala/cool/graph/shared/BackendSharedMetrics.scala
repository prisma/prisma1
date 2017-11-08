package cool.graph.shared

import cool.graph.metrics.{CustomTag, MetricsManager}

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

  val sqlQueryTimer = defineTimer("sqlQueryTimer", CustomTag("projectId", recordingThreshold = 1000))
}
