package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object ActionHandlerWebhook {
  lazy val Type: ObjectType[Unit, models.ActionHandlerWebhook] = ObjectType(
    "ActionHandlerWebhook",
    "This is an ActionHandlerWebhook",
    interfaces[Unit, models.ActionHandlerWebhook](nodeInterface),
    idField[Unit, models.ActionHandlerWebhook] ::
      fields[Unit, models.ActionHandlerWebhook](
      Field("url", StringType, resolve = _.value.url),
      Field("isAsync", BooleanType, resolve = _.value.isAsync)
    )
  )
}
