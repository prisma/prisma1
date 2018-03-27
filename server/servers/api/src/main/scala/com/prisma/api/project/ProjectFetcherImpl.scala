package com.prisma.api.project

import akka.http.scaladsl.model.Uri
import com.prisma.shared.models.ProjectJsonFormatter._
import com.prisma.shared.models.ProjectWithClientId
import com.prisma.twitterFutures.TwitterFutureImplicits._
import com.twitter.conversions.time._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectFetcherImpl(
    blockedProjectIds: Vector[String],
    schemaManagerEndpoint: String,
    schemaManagerSecret: String
) extends RefreshableProjectFetcher {

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
      case response                                => Some(Json.parse(response.getContentString()).as[ProjectWithClientId])
    }.asScala
  }
}
