package com.prisma.metrics.micrometer

import java.io.StringWriter
import java.net.URI

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

import scala.util.Try

object CustomPushGateway {
  def forAddress(address: String)(implicit as: ActorSystem): Try[CustomPushGateway] = {
    Try { URI.create(address) }.map { parsedUri =>
      val sanitized = address.stripSuffix("/")
      if (parsedUri.getScheme == null) {
        CustomPushGateway("https://" + sanitized) // Default to https
      } else {
        CustomPushGateway(sanitized)
      }
    }
  }
}

case class CustomPushGateway(address: String)(implicit as: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  val httpClient            = SimpleHttpClient()

  import as.dispatcher

  def pushAdd(collectorRegistry: CollectorRegistry, job: String, instance: String, secret: String): Unit = {
    httpClient
      .post(
        uri = s"$address/metrics/job/$job/instance/$instance",
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
