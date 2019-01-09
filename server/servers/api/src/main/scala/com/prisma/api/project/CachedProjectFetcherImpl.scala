package com.prisma.api.project

import com.prisma.api.ApiMetrics
import com.prisma.cache.Cache
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.{Everything, Message}
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

case class CachedProjectFetcherImpl(
    projectFetcher: RefreshableProjectFetcher,
    projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
)(implicit ec: ExecutionContext)
    extends RefreshableProjectFetcher {

  private val cache = Cache.lfuAsync[String, Project](initialCapacity = 16, maxCapacity = 100)

  projectSchemaInvalidationSubscriber.subscribe(
    Everything,
    (msg: Message[String]) => cache.remove(msg.payload)
  )

  override def fetch(projectIdOrAlias: String): Future[Option[Project]] = {
    ApiMetrics.projectCacheGetCount.inc()

    cache.getOrUpdateOpt(
      projectIdOrAlias,
      () => {
        ApiMetrics.projectCacheMissCount.inc()
        projectFetcher.fetch(projectIdOrAlias)
      }
    )
  }

  override def fetchRefreshed(projectIdOrAlias: String): Future[Option[Project]] = {
    val result = projectFetcher.fetchRefreshed(projectIdOrAlias)
    cache.put(projectIdOrAlias, result)
    result
  }
}
