package cool.graph.api.schema

import cool.graph.cache.Cache
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.{Everything, Message}
import cool.graph.shared.models.Project
import sangria.schema.Schema

case class CachedSchemaBuilder(
    schemaBuilder: SchemaBuilder,
    schemaInvalidationSubscriber: PubSubSubscriber[String]
) extends SchemaBuilder {
  private val cache = Cache.lfu[String, Schema[ApiUserContext, Unit]](initialCapacity = 16, maxCapacity = 50)

  schemaInvalidationSubscriber.subscribe(
    Everything,
    (msg: Message[String]) => cache.remove(msg.payload)
  )

  override def apply(project: Project): Schema[ApiUserContext, Unit] = {
    cache.getOrUpdate(project.id, () => schemaBuilder(project))
  }
}
