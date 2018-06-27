package com.prisma.metrics.prometheus

import java.io.StringWriter

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

object CustomPushGateway {
  def http(address: String)(implicit as: ActorSystem)  = CustomPushGateway("http", address)
  def https(address: String)(implicit as: ActorSystem) = CustomPushGateway("https", address)
}

case class CustomPushGateway(protocol: String, address: String)(implicit as: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  val httpClient            = SimpleHttpClient()

  import as.dispatcher

  val pushGatewayUrl = s"$protocol://$address/metrics/job/"

  def pushAdd(collectorRegistry: CollectorRegistry, job: String, secret: String): Unit = {
    httpClient
      .post(
        uri = pushGatewayUrl + job,
        body = prometheusBody(collectorRegistry),
        contentType = ContentTypes.`text/plain(UTF-8)`,
        headers = Vector("Authorization" -> secret)
      )
      .failed
      .foreach { e =>
        println("Push to Prometheus Gateway failed with: ")
        e.printStackTrace()
      }
  }

  private def prometheusBody(collector: CollectorRegistry): String = {
    val writer = new StringWriter()
    TextFormat.write004(writer, collector.metricFamilySamples)
    writer.toString
  }
}
