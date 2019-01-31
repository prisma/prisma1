package com.prisma.metrics.jvm

import java.lang.management.ManagementFactory

import com.prisma.metrics.MetricsRegistry
import com.sun.management.OperatingSystemMXBean

case class CpuProfiler(metricsRegistry: MetricsRegistry) {
  val mxBean = ManagementFactory.getOperatingSystemMXBean match {
    case x: OperatingSystemMXBean =>
      // docs for the bean available at https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html#getSystemCpuLoad--
      Some(x)

    case _ =>
      println("com.sun.management.OperatingSystemMXBean is not available on this JVM. CPU Metrics are therefore not available.")
      None
  }

  //  val processCpuLoad = metricsManager.defineGauge("processCpuLoadPercentage")
  val systemCpuLoad = metricsRegistry.defineGauge("systemCpuLoadPercentage")

  def profile(): Unit = {
    mxBean.foreach { mxBean =>
      //      processCpuLoad.set(convertToPercent(mxBean.getProcessCpuLoad))
      systemCpuLoad.set(convertToPercent(mxBean.getSystemCpuLoad))
    }
  }

  def convertToPercent(double: Double): Long = (double * 100).toLong
}
