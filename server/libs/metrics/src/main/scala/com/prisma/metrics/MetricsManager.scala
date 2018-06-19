package com.prisma.metrics

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
  private val prismaPushGatewayAddress = "192.168.1.10:80" // FIXME: use final address

  private[metrics] val meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  def init(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): Unit = {
    import as.dispatcher
    val pushGateway = CustomPushGateway.http(prismaPushGatewayAddress) // FIXME: use https

    as.scheduler.schedule(30.seconds, 30.seconds) {
      secretLoader.loadCloudSecret().onComplete {
        case Success(Some(secret)) => pushGateway.pushAdd(meterRegistry.getPrometheusRegistry, "prisma-connect", secret)
        case Success(None)         => log("No prismaConnectSecret is set. Metrics collection is disabled.")
        case Failure(e)            => e.printStackTrace()
      }
    }
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")
}

trait PrismaCloudSecretLoader {
  def loadCloudSecret(): Future[Option[String]]
}
