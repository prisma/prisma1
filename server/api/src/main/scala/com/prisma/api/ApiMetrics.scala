package com.prisma.api

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.{CustomTag, MetricsManager}
import com.prisma.profiling.JvmProfiler

object ApiMetrics extends MetricsManager(BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))) {
  // this is intentionally empty. Since we don't define metrics here, we need to load the object once so the profiler kicks in.
  // This way it does not look so ugly on the caller side.
  def init(): Unit = {}

  // CamelCase the service name read from env
  override def serviceName =
    sys.env
      .getOrElse("SERVICE_NAME", "Api")
      .split("-")
      .map { x =>
        x.head.toUpper + x.tail
      }
      .mkString

  JvmProfiler.schedule(this)

  val projectIdTag = CustomTag("projectId")

  val projectCacheGetCount          = defineCounter("projectCacheGetCount")
  val projectCacheMissCount         = defineCounter("projectCacheMissCount")
  val schemaBuilderBuildTimerMetric = defineTimer("schemaBuilderBuildTimer", CustomTag("projectId", recordingThreshold = 600))
  val sqlQueryTimer                 = defineTimer("sqlQueryTimer", CustomTag("projectId", recordingThreshold = 1000), CustomTag("queryName", recordingThreshold = 1000))
  val sqlDataChangeMutactionTimer   = defineTimer("sqlDataChangeMutactionTimer", CustomTag("projectId", recordingThreshold = 1000))
  val mutactionCount                = defineCounter("mutactionCount", CustomTag("projectId", recordingThreshold = 100))

  // these Metrics are consumed by the console to power the dashboard. Only change them with extreme caution!
  val requestDuration          = defineTimer("requestDuration", projectIdTag)
  val requestCounter           = defineCounter("requestCount", projectIdTag)
  val databaseSize             = defineGauge("databaseSize") // ???
  val subscriptionEventCounter = defineCounter("subscriptionEventCount", projectIdTag)
}
