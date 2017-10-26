package cool.graph.metrics

import cool.graph.profiling.MemoryProfiler

object ClientSharedMetrics extends MetricsManager {

  // CamelCase the service name read from env
  override def serviceName =
    sys.env
      .getOrElse("SERVICE_NAME", "ClientShared")
      .split("-")
      .map { x =>
        x.head.toUpper + x.tail
      }
      .mkString

  MemoryProfiler.schedule(this)

  val schemaBuilderBuildTimerMetric = defineTimer("schemaBuilderBuildTimer", CustomTag("projectId", recordingThreshold = 600))
}
