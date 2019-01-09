package com.prisma.api

import com.prisma.metrics.{CustomTag, MetricsManager}

object ApiMetrics extends MetricsManager {
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
