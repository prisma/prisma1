package com.prisma.metrics.micrometer

import java.util.UUID

import akka.actor.ActorSystem
import com.prisma.metrics._
import com.prisma.metrics.jvm.JvmProfiler
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MicrometerMetricsRegistry extends MetricsRegistry {
  private val prismaPushGatewayAddress = "metrics-eu1.prisma.io"
  private[metrics] val meterRegistry   = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val job                      = "prisma-connect"
  private var initialized              = false

  def initialize(secretLoader: PrismaCloudSecretLoader)(implicit as: ActorSystem): MetricsRegistry = {
    import as.dispatcher
    synchronized {
      if (initialized) return this

      JvmProfiler.schedule(this)

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

      initialized = true
      this
    }
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")

  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = MicrometerGaugeMetric(name, predefTags, meterRegistry)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = MicrometerCounterMetric(name, customTags, meterRegistry)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = MicrometerTimerMetric(name, customTags, meterRegistry)
}
