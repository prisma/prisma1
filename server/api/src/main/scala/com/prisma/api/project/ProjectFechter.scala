package cool.graph.api.project

import cool.graph.api.schema.APIErrors.ProjectNotFound
import cool.graph.shared.models.ProjectWithClientId

import scala.concurrent.{ExecutionContext, Future}

trait ProjectFetcher {
  def fetch_!(projectIdOrAlias: String)(implicit ec: ExecutionContext): Future[ProjectWithClientId] = {
    fetch(projectIdOrAlias = projectIdOrAlias) map {
      case None          => throw ProjectNotFound(projectIdOrAlias)
      case Some(project) => project
    }
  }

  def fetch(projectIdOrAlias: String): Future[Option[ProjectWithClientId]]
}

trait RefreshableProjectFetcher extends ProjectFetcher {
  def fetchRefreshed(projectIdOrAlias: String): Future[Option[ProjectWithClientId]]
}
