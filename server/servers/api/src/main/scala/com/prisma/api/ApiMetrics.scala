package com.prisma.api

import com.prisma.errors.BugsnagErrorReporter
import com.prisma.metrics.{CustomTag, MetricsManager}
import com.prisma.profiling.JvmProfiler

object ApiMetrics extends MetricsManager(BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))) {
  // this is intentionally empty. Since we don't define metrics here, we need to load the object once so the profiler kicks in.
  // This way it does not look so ugly on the caller side.
  def init(): Unit = {}

  // CamelCase the service name read from env
  override def serviceName = "Api"

  JvmProfiler.schedule(this)

  val projectCacheGetCount  = defineCounter("projectCache.get.count")
  val projectCacheMissCount = defineCounter("projectCache.miss.count")
  val schemaBuilderTimer    = defineTimer("schemaBuilder.time", CustomTag("projectId", recordingThreshold = 600))
  val mutactionTimer        = defineTimer("mutaction.time", CustomTag("projectId", recordingThreshold = 1000))
  val mutactionCount        = defineCounter("mutaction.count", CustomTag("projectId", recordingThreshold = 100))

  // these Metrics are consumed by the console to power the dashboard. Only change them with extreme caution!
  val projectIdTag             = CustomTag("projectId")
  val requestDuration          = defineTimer("request.time", projectIdTag)
  val requestCounter           = defineCounter("request.count", projectIdTag)
  val subscriptionEventCounter = defineCounter("subscription.event.count", projectIdTag)
}
