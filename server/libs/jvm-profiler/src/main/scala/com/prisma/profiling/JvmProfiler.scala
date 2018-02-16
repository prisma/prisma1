package com.prisma.profiling

import akka.actor.Cancellable
import com.prisma.metrics.MetricsManager

import scala.concurrent.duration.{FiniteDuration, _}

object JvmProfiler {
  def schedule(
      metricsManager: MetricsManager,
      initialDelay: FiniteDuration = 0.seconds,
      interval: FiniteDuration = 5.seconds
  ): Cancellable = {
    import metricsManager.gaugeFlushSystem.dispatcher
    val memoryProfiler = MemoryProfiler(metricsManager)
    val cpuProfiler    = CpuProfiler(metricsManager)
    metricsManager.gaugeFlushSystem.scheduler.schedule(initialDelay, interval) {
      memoryProfiler.profile()
      cpuProfiler.profile()
    }
  }
}
