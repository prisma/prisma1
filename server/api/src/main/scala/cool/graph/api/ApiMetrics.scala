package cool.graph.api

import cool.graph.metrics.{CustomTag, MetricsManager}
import cool.graph.profiling.MemoryProfiler

object ApiMetrics extends MetricsManager {
  // this is intentionally empty. Since we don't define metrics here, we need to load the object once so the profiler kicks in.
  // This way it does not look so ugly on the caller side.
  def init(): Unit = {}

  // CamelCase the service name read from env
  override def serviceName =
    sys.env
      .getOrElse("SERVICE_NAME", "SystemShared")
      .split("-")
      .map { x =>
        x.head.toUpper + x.tail
      }
      .mkString

  MemoryProfiler.schedule(this)

  val projectCacheGetCount          = defineCounter("projectCacheGetCount")
  val projectCacheMissCount         = defineCounter("projectCacheMissCount")
  val schemaBuilderBuildTimerMetric = defineTimer("schemaBuilderBuildTimer", CustomTag("projectId", recordingThreshold = 600))
  val sqlQueryTimer                 = defineTimer("sqlQueryTimer", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))
  val sqlDataChangeMutactionTimer   = defineTimer("sqlDataChangeMutactionTimer", CustomTag("projectId", recordingThreshold = 1000))
  val requestDuration               = defineTimer("requestDuration", CustomTag("projectId", recordingThreshold = 1500))

}
