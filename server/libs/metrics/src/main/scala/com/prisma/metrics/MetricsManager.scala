package com.prisma.metrics

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import com.librato.metrics.client.{Duration, LibratoClient}
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.errors.ErrorReporter
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.exporter.PushGateway

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

abstract class MetricsManager(reporter: ErrorReporter) {
  def serviceName: String

  // System used to periodically flush the state of individual gauges
  implicit lazy val gaugeFlushSystem: ActorSystem = SingleThreadedActorSystem(s"$serviceName-gauges")

  lazy val errorHandler = CustomErrorHandler()(reporter)

  private val metricsCollectionIsEnabled: Boolean = sys.env.getOrElse("ENABLE_METRICS", "0") == "1"

  private def log(msg: String): Unit = println(s"[Metrics] $msg")

  private val prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val pushGateway        = new PushGateway("localhost:9091")

  gaugeFlushSystem.scheduler.schedule(30.seconds, 30.seconds) {
    pushGateway.pushAdd(prometheusRegistry.getPrometheusRegistry, "samples")
  }(gaugeFlushSystem.dispatcher)

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, predefTags, prometheusRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, customTags, prometheusRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, customTags, prometheusRegistry)

  def shutdown: Unit = Await.result(gaugeFlushSystem.terminate(), 10.seconds)
}
