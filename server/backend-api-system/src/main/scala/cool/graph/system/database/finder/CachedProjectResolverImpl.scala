package cool.graph.system.database.finder

import cool.graph.cache.Cache
import cool.graph.shared.models.{Project, ProjectWithClientId}

import scala.concurrent.{ExecutionContext, Future}

case class CachedProjectResolverImpl(
    uncachedProjectResolver: UncachedProjectResolver
)(implicit ec: ExecutionContext)
    extends CachedProjectResolver {
  val cache = Cache.lfuAsync[String, ProjectWithClientId](initialCapacity = 5, maxCapacity = 5)

  override def resolve(projectIdOrAlias: String): Future[Option[Project]] = resolveProjectWithClientId(projectIdOrAlias).map(_.map(_.project))

  override def resolveProjectWithClientId(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = {
    cache.getOrUpdateOpt(projectIdOrAlias, () => {
      uncachedProjectResolver.resolveProjectWithClientId(projectIdOrAlias)
    })
  }

  override def invalidate(projectIdOrAlias: String): Future[Unit] = {
    cache.remove(projectIdOrAlias)
    Future.successful(())
  }
}
