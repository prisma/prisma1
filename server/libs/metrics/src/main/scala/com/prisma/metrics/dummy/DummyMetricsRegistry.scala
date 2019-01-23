package com.prisma.metrics.dummy

import akka.actor.ActorSystem
import com.prisma.metrics._

import scala.concurrent.{ExecutionContext, Future}

object DummyMetricsRegistry extends MetricsRegistry {
  def initialize(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): MetricsRegistry = this

  override def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = DummyGaugeMetric
  override def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = DummyCounterMetric
  override def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = DummyTimerMetric
}

object DummyGaugeMetric extends GaugeMetric {
  override def inc: Unit                 = {}
  override def dec: Unit                 = {}
  override def add(delta: Long): Unit    = {}
  override def set(fixedVal: Long): Unit = {}
  override def get: Long                 = 0

  override val name: String               = "DummyGauge"
  override val customTags: Seq[CustomTag] = Seq.empty
}

object DummyCounterMetric extends CounterMetric {
  override def inc(customTagValues: String*): Unit                = {}
  override def incBy(delta: Long, customTagValues: String*): Unit = {}

  override val name: String               = "DummyCounter"
  override val customTags: Seq[CustomTag] = Seq.empty
}

object DummyTimerMetric extends TimerMetric {
  override def timeFuture[T](customTagValues: String*)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = f
  override def time[T](customTagValues: String*)(f: => T): T                                                      = f
  override def record(timeMillis: Long, customTagValues: Seq[String]): Unit                                       = {}

  override val name: String               = "DummyTimer"
  override val customTags: Seq[CustomTag] = Seq.empty
}
