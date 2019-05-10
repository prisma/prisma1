package com.prisma.metrics.jvm

import java.lang.management.{GarbageCollectorMXBean, ManagementFactory, MemoryUsage}

import com.prisma.metrics.MetricsRegistry
import com.sun.management.ThreadMXBean

import scala.util.{Failure, Success, Try}

case class MemoryProfiler(metricsRegistry: MetricsRegistry) {
  import scala.collection.JavaConverters._

  val garbageCollectionMetrics = asScalaBuffer(ManagementFactory.getGarbageCollectorMXBeans).map(gcBean => GarbageCollectionMetrics(metricsRegistry, gcBean))
  val memoryMxBean             = ManagementFactory.getMemoryMXBean
  val heapMemoryMetrics        = MemoryMetrics(metricsRegistry, initialMemoryUsage = memoryMxBean.getHeapMemoryUsage, prefix = "heap")
  val offHeapMemoryMetrics     = MemoryMetrics(metricsRegistry, initialMemoryUsage = memoryMxBean.getNonHeapMemoryUsage, prefix = "off-heap")
  val allocationMetrics = {
    val threadMxBean = ManagementFactory.getThreadMXBean match {
      case x: ThreadMXBean =>
        Some(x)
      case _ =>
        println("com.sun.management.ThreadMXBean is not available on this JVM. Memory Allocation Metrics are therefore not available.")
        None
    }
    AllocationMetrics(metricsRegistry, threadMxBean)
  }

  def profile(): Unit = {
    heapMemoryMetrics.record(memoryMxBean.getHeapMemoryUsage)
    offHeapMemoryMetrics.record(memoryMxBean.getNonHeapMemoryUsage)
    garbageCollectionMetrics.foreach(_.record)
    allocationMetrics.record()
  }
}

case class AllocationMetrics(metricsRegistry: MetricsRegistry, mxBean: Option[ThreadMXBean]) {
  // docs for the bean: https://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/ThreadMXBean.html#getThreadAllocatedBytes(long)

  val allocationRate = metricsRegistry.defineCounter("memoryAllocationRateInKb")

  val isThreadAllocatedMemoryEnabled = Try {
    mxBean match {
      case Some(mxBean) => mxBean.isThreadAllocatedMemoryEnabled
      case None         => false
    }
  } match {
    case Success(x) => x
    case Failure(_) => false
  }

  var lastAllocatedBytes = Map.empty[Long, Long] // thread id to allocated kilobytes

  def record(): Unit = {
    if (isThreadAllocatedMemoryEnabled) {
      mxBean.foreach { mxBean =>
        recordAllocatedBytes(mxBean)
      }
    }
  }

  def recordAllocatedBytes(mxBean: ThreadMXBean): Unit = {
    val newLastAllocatedBytes = mxBean.getAllThreadIds.map { id =>
      val bytes       = mxBean.getThreadAllocatedBytes(id)
      val inKiloBytes = if (bytes > 0) bytes / 1024 else 0
      id -> inKiloBytes
    }.toMap

    newLastAllocatedBytes.foreach {
      case (id, bytes: Long) =>
        val lastBytes: Long = lastAllocatedBytes.getOrElse(id, 0)
        val delta           = bytes - lastBytes
        allocationRate.incBy(delta)
    }

    lastAllocatedBytes = newLastAllocatedBytes
  }
}

case class MemoryMetrics(metricsRegistry: MetricsRegistry, initialMemoryUsage: MemoryUsage, prefix: String) {
  val initialMemory   = metricsRegistry.defineGauge(s"$prefix.initial")
  val usedMemory      = metricsRegistry.defineGauge(s"$prefix.used")
  val committedMemory = metricsRegistry.defineGauge(s"$prefix.committed")
  val maxMemory       = metricsRegistry.defineGauge(s"$prefix.max")

  // those don't change over time and we don't want to report them again and again
  initialMemory.set(Math.max(initialMemoryUsage.getInit, 0))
  maxMemory.set(initialMemoryUsage.getMax)

  def record(memoryUsage: MemoryUsage): Unit = {
    committedMemory.set(memoryUsage.getCommitted)
    usedMemory.set(memoryUsage.getUsed)
  }
}

case class GarbageCollectionMetrics(metricsRegistry: MetricsRegistry, gcBean: GarbageCollectorMXBean) {
  var lastCount: Long = 0
  var lastTime: Long  = 0

  val countMetric = metricsRegistry.defineCounter("gc." + gcBean.getName + ".collectionCount")
  val timeMetric  = metricsRegistry.defineTimer("gc." + gcBean.getName + ".collectionTime")

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
