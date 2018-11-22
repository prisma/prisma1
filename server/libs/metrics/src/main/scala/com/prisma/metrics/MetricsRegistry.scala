package com.prisma.metrics

import akka.actor.ActorSystem
import scala.annotation.varargs
import scala.concurrent.Future

//trait MetricsFacade {
//  def manager: MetricsRegistry = MicrometerMetricsRegistry
//
//  def initialize(secretLoader: PrismaCloudSecretLoader)(implicit system: ActorSystem): Unit = manager.initialize(secretLoader)
//  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric              = manager.defineGauge(name, predefTags: _*)
//  def defineCounter(name: String, customTags: CustomTag*): CounterMetric                    = manager.defineCounter(name, customTags: _*)
//  def defineTimer(name: String, customTags: CustomTag*): TimerMetric                        = manager.defineTimer(name, customTags: _*)
//}

trait MetricsRegistry {
  def initialize(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): Unit

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  @varargs def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric
  @varargs def defineCounter(name: String, customTags: CustomTag*): CounterMetric
  @varargs def defineTimer(name: String, customTags: CustomTag*): TimerMetric
}

trait PrismaCloudSecretLoader {
  def loadCloudSecret(): Future[Option[String]]
}
