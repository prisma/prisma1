package com.prisma.metrics

import java.util.UUID

import akka.actor.ActorSystem
import com.prisma.metrics.prometheus.CustomPushGateway
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait MetricsManager {
  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, predefTags, MetricsRegistry.meterRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, customTags, MetricsRegistry.meterRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, customTags, MetricsRegistry.meterRegistry)
}

object DefaultMetricsManager extends MetricsManager

object MetricsRegistry {
  private val prismaPushGatewayAddress = "metrics-eu1.prisma.io"
  private[metrics] val meterRegistry   = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val job                      = "prisma-connect"

  def init(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): Unit = {
    import as.dispatcher
    CustomPushGateway.forAddress(prismaPushGatewayAddress) match {
      case Success(pushGateway) =>
        val instanceKey = UUID.randomUUID().toString
        as.scheduler.schedule(30.seconds, 30.seconds) {
          secretLoader.loadCloudSecret().onComplete {
            case Success(Some(secret)) => pushGateway.pushAdd(meterRegistry.getPrometheusRegistry, job, instanceKey, secret)
            case Success(None)         => // NO-OP
            case Failure(e)            => e.printStackTrace()
          }
        }

      case Failure(err) =>
        println(s"[Metrics] Error during init: $err. Metrics collection is disabled")
    }
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")
}

trait PrismaCloudSecretLoader {
  def loadCloudSecret(): Future[Option[String]]
}
