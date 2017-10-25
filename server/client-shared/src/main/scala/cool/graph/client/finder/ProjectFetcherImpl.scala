package cool.graph.client.finder

import akka.http.scaladsl.model.Uri
import com.twitter.conversions.time._
import com.typesafe.config.Config
import cool.graph.shared.SchemaSerializer
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.twitterFutures.TwitterFutureImplicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectFetcherImpl(
    blockedProjectIds: Vector[String],
    config: Config
) extends RefreshableProjectFetcher {
  private val schemaManagerEndpoint = config.getString("schemaManagerEndpoint")
  private val schemaManagerSecret   = config.getString("schemaManagerSecret")

  private lazy val schemaService = {
    val client = if (schemaManagerEndpoint.startsWith("https")) {
      com.twitter.finagle.Http.client.withTls(Uri(schemaManagerEndpoint).authority.host.address())
    } else {
      com.twitter.finagle.Http.client
    }

    val destination = s"${Uri(schemaManagerEndpoint).authority.host}:${Uri(schemaManagerEndpoint).effectivePort}"
    client.withRequestTimeout(10.seconds).newService(destination)
  }

  override def fetchRefreshed(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = fetch(projectIdOrAlias, forceRefresh = true)
  override def fetch(projectIdOrAlias: String): Future[Option[ProjectWithClientId]]          = fetch(projectIdOrAlias, forceRefresh = false)

  /**
    * Loads schema from backend-api-schema-manager service.
    */
  private def fetch(projectIdOrAlias: String, forceRefresh: Boolean): Future[Option[ProjectWithClientId]] = {
    if (blockedProjectIds.contains(projectIdOrAlias)) {
      return Future.successful(None)
    }

    // load from backend-api-schema-manager service
    val uri = forceRefresh match {
      case true  => s"$schemaManagerEndpoint/$projectIdOrAlias?forceRefresh=true"
      case false => s"$schemaManagerEndpoint/$projectIdOrAlias"
    }

    val request = com.twitter.finagle.http
      .RequestBuilder()
      .url(uri)
      .addHeader("Authorization", s"Bearer $schemaManagerSecret")
      .buildGet()

    // schema deserialization failure should blow up as we have no recourse
    schemaService(request).map {
      case response if response.status.code >= 400 => None
      case response                                => Some(SchemaSerializer.deserializeProjectWithClientId(response.getContentString()).get)
    }.asScala
  }
}
