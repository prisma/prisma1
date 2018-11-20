package com.prisma.api.schema

import com.prisma.cache.factory.CacheFactory
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.{Everything, Message}
import com.prisma.shared.models.Project
import sangria.schema.Schema

case class CachedSchemaBuilder(
    schemaBuilder: SchemaBuilder,
    schemaInvalidationSubscriber: PubSubSubscriber[String],
    cacheFactory: CacheFactory
) extends SchemaBuilder {
  private val cache = cacheFactory.lfu[String, Schema[ApiUserContext, Unit]](initialCapacity = 16, maxCapacity = 50)

  schemaInvalidationSubscriber.subscribe(
    Everything,
    (msg: Message[String]) => cache.remove(msg.payload)
  )

  override def apply(project: Project): Schema[ApiUserContext, Unit] = {
    cache.getOrUpdate(project.id, () => schemaBuilder(project))
  }
}
