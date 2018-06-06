package com.prisma.metrics

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import com.librato.metrics.client.{Duration, LibratoClient}
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.errors.ErrorReporter
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

abstract class MetricsManager(reporter: ErrorReporter) {
  def serviceName: String

  // System used to periodically flush the state of individual gauges
  implicit lazy val gaugeFlushSystem: ActorSystem = SingleThreadedActorSystem(s"$serviceName-gauges")

  lazy val errorHandler = CustomErrorHandler()(reporter)

  private val metricsCollectionIsEnabled: Boolean = sys.env.getOrElse("ENABLE_METRICS", "0") == "1"

  protected lazy val baseTags: Map[String, String] = {
    if (metricsCollectionIsEnabled) {
      Try {
        Map(
          "env"       -> sys.env.getOrElse("ENV", "local"),
          "region"    -> sys.env.getOrElse("AWS_REGION", "no_region"),
          "container" -> ContainerMetadata.fetchContainerId(),
          "service"   -> serviceName
        )
      } match {
        case Success(tags) => tags
        case Failure(err)  => errorHandler.handle(new Exception(err)); Map.empty
      }
    } else {
      Map.empty
    }
  }
  protected lazy val baseTagsString: String = {
    baseTags
      .map {
        case (key, value) => s"$key=$value"
      }
      .mkString(",")
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")

  private val prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, baseTagsString, predefTags, prometheusRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, baseTagsString, customTags, prometheusRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, baseTagsString, customTags, prometheusRegistry)

  def shutdown: Unit = Await.result(gaugeFlushSystem.terminate(), 10.seconds)
}
