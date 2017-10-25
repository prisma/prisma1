package cool.graph.metrics

import com.twitter.finagle
import com.twitter.finagle.http.{Method, Request, Response}
import cool.graph.metrics.Utils._

import scala.concurrent.Future
import com.twitter.conversions.time._
import com.twitter.finagle.service.Backoff

object InstanceMetadata {
  import scala.concurrent.ExecutionContext.Implicits.global

  val service = finagle.Http.client.withRetryBackoff(Backoff.const(5.seconds)).withRequestTimeout(15.seconds).newService("169.254.169.254:80")

  /**
    * Fetches the EC2 IP of the host VM using the EC2 metadata service.
    *
    * @return A future containing the host IP as string.
    */
  def fetchInstanceIP(): Future[String] = fetch("/latest/meta-data/local-ipv4")

  /**
    * Fetches the EC2 ami launch index of the host VM using the EC2 metadata service.
    *
    * @return A future containing the ami launch index as string.
    */
  def fetchInstanceLaunchIndex(): Future[String] = fetch("/latest/meta-data/ami-launch-index")

  /**
    * Fetches the EC2 instance ID of the host VM using the EC2 metadata service.
    *
    * @return A future containing the instance ID as string.
    */
  def fetchInstanceId(): Future[String] = fetch("/latest/meta-data/instance-id")

  /**
    * Generic fetch for the metadata service.
    *
    * @param path The path on the ecs metadata service to fetch (including leading slash)
    * @return The content as string of the response
    */
  private def fetch(path: String): Future[String] = {
    val request       = Request(Method.Get, path)
    val requestFuture = service(request).asScala

    requestFuture.onFailure({
      case e => throw MetricsError(s"Error while fetching request ${request.uri}: $e")
    })

    requestFuture.map { (response: Response) =>
      response.status match {
        case x if x.code >= 200 && x.code < 300 =>
          val ip = response.contentString
          println(s"[Metrics] Request ${request.uri} result: $ip")
          ip

        case _ =>
          throw MetricsError(s"Unable to retrieve EC2 metadata (${request.uri}) - ${response.status} | ${response.contentString}")
      }
    }
  }
}
