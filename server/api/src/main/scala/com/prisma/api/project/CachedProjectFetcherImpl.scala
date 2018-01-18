package com.prisma.api.project

import com.prisma.api.ApiMetrics
import com.prisma.cache.Cache
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.{Everything, Message}
import com.prisma.shared.models.ProjectWithClientId

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
