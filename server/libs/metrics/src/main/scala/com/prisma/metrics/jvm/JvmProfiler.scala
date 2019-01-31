package com.prisma.metrics.jvm

import akka.actor.{ActorSystem, Cancellable}
import com.prisma.metrics.MetricsRegistry

import scala.concurrent.duration.{FiniteDuration, _}

object JvmProfiler {
  def schedule(
      metricsRegistry: MetricsRegistry,
      initialDelay: FiniteDuration = 0.seconds,
      interval: FiniteDuration = 5.seconds
  )(implicit as: ActorSystem): Cancellable = {
    import as.dispatcher

    val memoryProfiler = MemoryProfiler(metricsRegistry)
    val cpuProfiler    = CpuProfiler(metricsRegistry)

    as.scheduler.schedule(initialDelay, interval) {
      memoryProfiler.profile()
      cpuProfiler.profile()
    }
  }
}
