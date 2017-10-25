package cool.graph.profiling

import java.lang.management.{GarbageCollectorMXBean, ManagementFactory, MemoryUsage}

import akka.actor.Cancellable
import cool.graph.metrics.MetricsManager

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object MemoryProfiler {
  def schedule(
      metricsManager: MetricsManager,
      initialDelay: FiniteDuration = 0.seconds,
      interval: FiniteDuration = 5.seconds
  ): Cancellable = {
    import metricsManager.gaugeFlushSystem.dispatcher
    val profiler = MemoryProfiler(metricsManager)
    metricsManager.gaugeFlushSystem.scheduler.schedule(initialDelay, interval) {
      profiler.profile()
    }
  }
}

case class MemoryProfiler(metricsManager: MetricsManager) {
  import scala.collection.JavaConversions._

  val garbageCollectionMetrics = ManagementFactory.getGarbageCollectorMXBeans.map(gcBean => GarbageCollectionMetrics(metricsManager, gcBean))
  val memoryMxBean             = ManagementFactory.getMemoryMXBean
  val heapMemoryMetrics        = MemoryMetrics(metricsManager, initialMemoryUsage = memoryMxBean.getHeapMemoryUsage, prefix = "heap")
  val offHeapMemoryMetrics     = MemoryMetrics(metricsManager, initialMemoryUsage = memoryMxBean.getNonHeapMemoryUsage, prefix = "off-heap")

  def profile(): Unit = {
    heapMemoryMetrics.record(memoryMxBean.getHeapMemoryUsage)
    offHeapMemoryMetrics.record(memoryMxBean.getNonHeapMemoryUsage)
    garbageCollectionMetrics.foreach(_.record)
  }
}

case class MemoryMetrics(metricsManager: MetricsManager, initialMemoryUsage: MemoryUsage, prefix: String) {
  val initialMemory   = metricsManager.defineGauge(s"$prefix.initial")
  val usedMemory      = metricsManager.defineGauge(s"$prefix.used")
  val committedMemory = metricsManager.defineGauge(s"$prefix.committed")
  val maxMemory       = metricsManager.defineGauge(s"$prefix.max")

  // those don't change over time and we don't want to report them again and again
  initialMemory.set(Math.max(initialMemoryUsage.getInit, 0))
  maxMemory.set(initialMemoryUsage.getMax)

  def record(memoryUsage: MemoryUsage): Unit = {
    committedMemory.set(memoryUsage.getCommitted)
    usedMemory.set(memoryUsage.getUsed)
  }
}

case class GarbageCollectionMetrics(metricsManager: MetricsManager, gcBean: GarbageCollectorMXBean) {
  var lastCount: Long = 0
  var lastTime: Long  = 0

  val countMetric = metricsManager.defineCounter("gc." + gcBean.getName + ".collectionCount")
  val timeMetric  = metricsManager.defineTimer("gc." + gcBean.getName + ".collectionTime")

  def record(): Unit = {
    recordGcCount
    recordGcTime
  }

  private def recordGcCount(): Unit = {
    val newGcCount  = gcBean.getCollectionCount
    val lastGcCount = lastCount
    countMetric.incBy(newGcCount - lastGcCount)
    lastCount = newGcCount
  }

  private def recordGcTime(): Unit = {
    val newGcTime  = gcBean.getCollectionTime
    val lastGcTime = lastTime
    timeMetric.record(newGcTime - lastGcTime)
    lastTime = newGcTime
  }
}
