package com.prisma.api

import com.prisma.metrics.{CustomTag, MetricsFacade, MetricsRegistry, TimerMetric}

object ApiMetrics extends MetricsFacade {
  def init(metricsRegistry: MetricsRegistry): Unit = registry = metricsRegistry

  lazy val projectCacheGetCount      = defineCounter("projectCache.get.count")
  lazy val projectCacheMissCount     = defineCounter("projectCache.miss.count")
  lazy val schemaBuilderTimer        = defineTimer("schemaBuilder.time", CustomTag("projectId", recordingThreshold = 600))
  lazy val mutactionTimer            = defineTimer("mutaction.time", CustomTag("projectId", recordingThreshold = 1000))
  lazy val mutactionCount            = defineCounter("mutaction.count", CustomTag("projectId", recordingThreshold = 100))
  lazy val requestTimer: TimerMetric = defineTimer("responseTime", CustomTag("status"))

  // these Metrics are consumed by the console to power the cloud dashboard. Only change them with extreme caution!
  lazy val projectIdTag             = CustomTag("projectId")
  lazy val requestDuration          = defineTimer("request.time", projectIdTag)
  lazy val requestCounter           = defineCounter("request.count", projectIdTag)
  lazy val subscriptionEventCounter = defineCounter("subscription.event.count", projectIdTag)
}
