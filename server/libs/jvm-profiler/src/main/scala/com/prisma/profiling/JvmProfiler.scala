package com.prisma.profiling

import akka.actor.{ActorSystem, Cancellable}
import com.prisma.metrics.MetricsManager

import scala.concurrent.duration.{FiniteDuration, _}

object JvmProfiler {
  def schedule(
      metricsManager: MetricsManager,
      initialDelay: FiniteDuration = 0.seconds,
      interval: FiniteDuration = 5.seconds
  )(implicit as: ActorSystem): Cancellable = {
    import as.dispatcher
    val memoryProfiler = MemoryProfiler(metricsManager)
    val cpuProfiler    = CpuProfiler(metricsManager)
    as.scheduler.schedule(initialDelay, interval) {
      memoryProfiler.profile()
      cpuProfiler.profile()
    }
  }
}
