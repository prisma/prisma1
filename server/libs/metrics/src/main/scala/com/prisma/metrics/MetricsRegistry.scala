package com.prisma.metrics

import akka.actor.ActorSystem
import scala.annotation.varargs
import scala.concurrent.Future

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
