package com.prisma.metrics

import com.prisma.metrics.dummy.DummyMetricsRegistry

import scala.annotation.varargs
import scala.concurrent.Future

trait MetricsFacade {
  var registry: MetricsRegistry = DummyMetricsRegistry

  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = registry.defineGauge(name, predefTags: _*)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = registry.defineCounter(name, customTags: _*)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = registry.defineTimer(name, customTags: _*)
}

trait MetricsRegistry {
  //  def initialize(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): Unit

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  @varargs def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric
  @varargs def defineCounter(name: String, customTags: CustomTag*): CounterMetric
  @varargs def defineTimer(name: String, customTags: CustomTag*): TimerMetric
}

trait PrismaCloudSecretLoader {
  def loadCloudSecret(): Future[Option[String]]
}
