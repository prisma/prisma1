package cool.graph.api.project

import cool.graph.api.ApiMetrics
import cool.graph.cache.Cache
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.{Everything, Message}
import cool.graph.shared.models.ProjectWithClientId

import scala.concurrent.Future

case class CachedProjectFetcherImpl(
    projectFetcher: RefreshableProjectFetcher,
    projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
) extends RefreshableProjectFetcher {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val cache = Cache.lfuAsync[String, ProjectWithClientId](initialCapacity = 16, maxCapacity = 100)

  projectSchemaInvalidationSubscriber.subscribe(
    Everything,
    (msg: Message[String]) => cache.remove(msg.payload)
  )

  override def fetch(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = {
    ApiMetrics.projectCacheGetCount.inc()

    cache.getOrUpdateOpt(
      projectIdOrAlias,
      () => {
        ApiMetrics.projectCacheMissCount.inc()
        projectFetcher.fetch(projectIdOrAlias)
      }
    )
  }

  override def fetchRefreshed(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = {
    val result = projectFetcher.fetchRefreshed(projectIdOrAlias)
    cache.put(projectIdOrAlias, result)
    result
  }
}
