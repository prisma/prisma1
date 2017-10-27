package cool.graph.client.finder

import cool.graph.cache.Cache
import cool.graph.client.metrics.BackendSharedMetrics
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.{Everything, Message}
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.utils.future.FutureUtils._

import scala.concurrent.Future
import scala.util.Success

case class CachedProjectFetcherImpl(
                                     projectFetcher: RefreshableProjectFetcher,
                                     projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
                                   ) extends RefreshableProjectFetcher {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val cache = Cache.lfuAsync[String, ProjectWithClientId](initialCapacity = 16, maxCapacity = 100)
  // ideally i would like to install a callback on cache for evictions. Whenever a project gets evicted i would remove it from the mapping cache as well.
  // This would make sure the mapping is always up-to-date and does not grow unbounded and causes memory problems.
  // So instead i am constraining the capacity to at least prohibit unbounded growth.
  private val aliasToIdMapping = Cache.lfu[String, String](initialCapacity = 16, maxCapacity = 200)

  projectSchemaInvalidationSubscriber.subscribe(
    Everything,
    (msg: Message[String]) => {

      val projectWithClientId: Future[Option[ProjectWithClientId]] = cache.get(msg.payload)

      projectWithClientId.toFutureTry
        .flatMap {
          case Success(Some(p)) =>
            val alias: Option[String] = p.project.alias
            alias.foreach(a => aliasToIdMapping.remove(a))
            Future.successful(())

          case _ =>
            Future.successful(())
        }
        .map(_ => cache.remove(msg.payload))
    }
  )

  override def fetch(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = {
    BackendSharedMetrics.projectCacheGetCount.inc()
    val potentialId = aliasToIdMapping.get(projectIdOrAlias).getOrElse(projectIdOrAlias)

    cache.getOrUpdateOpt(
      potentialId,
      () => {
        BackendSharedMetrics.projectCacheMissCount.inc()
        fetchProjectAndUpdateMapping(potentialId)(projectFetcher.fetch)
      }
    )
  }

  override def fetchRefreshed(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = {
    val result = fetchProjectAndUpdateMapping(projectIdOrAlias)(projectFetcher.fetchRefreshed)
    cache.put(projectIdOrAlias, result)
    result
  }

  private def fetchProjectAndUpdateMapping(projectIdOrAlias: String)(fn: String => Future[Option[ProjectWithClientId]]): Future[Option[ProjectWithClientId]] = {
    val result = fn(projectIdOrAlias)
    result.onSuccess {
      case Some(ProjectWithClientId(project, _)) =>
        project.alias.foreach { alias =>
          aliasToIdMapping.put(alias, project.id)
        }
    }
    result
  }
}