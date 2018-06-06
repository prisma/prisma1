package com.prisma.metrics

import akka.actor.ActorSystem
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.config.ConfigLoader
import com.prisma.errors.ErrorReporter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.exporter.PushGateway

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class MetricsManager(reporter: ErrorReporter) {
  def serviceName: String

  // System used to periodically flush the metrics
  implicit lazy val gaugeFlushSystem: ActorSystem = SingleThreadedActorSystem(s"$serviceName-gauges")

  private def log(msg: String): Unit = println(s"[Metrics] $msg")

  private val meterRegistry = ConfigLoader.load().prismaConnectSecret match {
    case Some(secret) =>
      val registry    = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT) // TODO: create dummy if metrics collection is disabled
      val pushGateway = new PushGateway("192.168.1.10:80")

      gaugeFlushSystem.scheduler.schedule(30.seconds, 30.seconds) {
        pushGateway.pushAdd(registry.getPrometheusRegistry, "samples")
        println("-" * 75)
        println(registry.scrape())
      }(gaugeFlushSystem.dispatcher)

      registry

    case None =>
      log("No prismaConnectSecret is set. Metrics collection is disabled.")
      new SimpleMeterRegistry()
  }
  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, predefTags, meterRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, customTags, meterRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, customTags, meterRegistry)

  def shutdown: Unit = Await.result(gaugeFlushSystem.terminate(), 10.seconds)
}
