package com.prisma.metrics

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import com.librato.metrics.client.{Duration, LibratoClient}
import com.prisma.errors.ErrorReporter
import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import com.prisma.akkautil.SingleThreadedActorSystem

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

  protected val client: StatsDClient = {
    if (metricsCollectionIsEnabled) {
      val dnsNameOpt = sys.env.get("STATSD_DNS_NAME")
      val portOpt    = Utils.envVarAsInt("STATSD_PORT")

      (dnsNameOpt, portOpt) match {
        case (Some(dnsName), Some(port)) =>
          log(s"Will report metrics to $dnsName on port $port")
          new NonBlockingStatsDClient("", Integer.MAX_VALUE, new Array[String](0), errorHandler, StatsdHostLookup(dnsName, port))

        case _ =>
          log("Warning: no metrics will be recorded. The env vars STATSD_DNS_NAME and STATSD_PORT must be set.")
          DummyStatsDClient()
      }
    } else {
      log("Warning: no metrics will be recorded.")
      DummyStatsDClient()
    }
  }

  private lazy val libratoReporter = {
    val email = Utils.envVar_!("LIBRATO_EMAIL")
    val token = Utils.envVar_!("LIBRATO_TOKEN")
    val client = LibratoClient
      .builder(email, token)
      .setConnectTimeout(new Duration(5, TimeUnit.SECONDS))
      .setReadTimeout(new Duration(5, TimeUnit.SECONDS))
      .setAgentIdentifier("my app name")
      .build()
    val actorRef = gaugeFlushSystem.actorOf(Props(LibratoFlushActor(client)))
    LibratoReporter(actorRef)
  }

  private def log(msg: String): Unit = println(s"[Metrics] $msg")

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(name, baseTagsString, predefTags, client)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(name, baseTagsString, customTags, client)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(name, baseTagsString, customTags, client)

  def defineLibratoGauge(name: String, flushInterval: FiniteDuration, predefTags: (CustomTag, String)*): LibratoGaugeMetric = {
    LibratoGaugeMetric(name, baseTags, predefTags, libratoReporter, flushInterval)
  }

  def shutdown: Unit = Await.result(gaugeFlushSystem.terminate(), 10.seconds)
}
