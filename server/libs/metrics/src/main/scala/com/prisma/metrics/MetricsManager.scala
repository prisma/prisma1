package com.prisma.metrics

import akka.actor.ActorSystem
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.config.ConfigLoader
import com.prisma.metrics.prometheus.CustomPushGateway
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

import scala.concurrent.duration._

trait MetricsManager {
  private def meterRegistry = MetricsRegistry.instance

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, predefTags, meterRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, customTags, meterRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, customTags, meterRegistry)
}

object DefaultMetricsManager extends MetricsManager

object MetricsRegistry {
  private val prismaPushGateway = "metrics-eu1.prisma.io"

  // System used to periodically flush the metrics
  implicit lazy val gaugeFlushSystem: ActorSystem = SingleThreadedActorSystem(s"metrics-manager")

  private[metrics] val instance = ConfigLoader.load().prismaConnectSecret match {
    case Some(secret) =>
      val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
      val pushGateway = CustomPushGateway.https(prismaPushGateway, secret)

      gaugeFlushSystem.scheduler.schedule(30.seconds, 30.seconds) {
        pushGateway.pushAdd(registry.getPrometheusRegistry, "prisma-connect")
      }(gaugeFlushSystem.dispatcher)

      registry

    case None =>
      log("No prismaConnectSecret is set. Metrics collection is disabled.")
      new SimpleMeterRegistry()
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")
}
