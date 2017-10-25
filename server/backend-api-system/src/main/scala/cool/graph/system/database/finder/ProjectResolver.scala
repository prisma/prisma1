package cool.graph.system.database.finder

import cool.graph.shared.models.{Project, ProjectWithClientId}

import scala.concurrent.Future

trait ProjectResolver {
  def resolve(projectIdOrAlias: String): Future[Option[Project]]
  def resolveProjectWithClientId(projectIdOrAlias: String): Future[Option[ProjectWithClientId]]
}

trait CachedProjectResolver extends ProjectResolver {

  /**
    * Invalidates the cache entry for the given project id or alias.
    */
  def invalidate(projectIdOrAlias: String): Future[Unit]
}
