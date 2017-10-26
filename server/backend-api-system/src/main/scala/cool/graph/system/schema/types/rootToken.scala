package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object rootToken {
  lazy val Type: ObjectType[Unit, models.RootToken] = ObjectType(
    "PermanentAuthToken",
    "Used to grant permanent access to your applications and services",
    interfaces[Unit, models.RootToken](nodeInterface),
    idField[Unit, models.RootToken] ::
      fields[Unit, models.RootToken](
      Field("name", StringType, resolve = _.value.name),
      Field("token", StringType, resolve = _.value.token)
    )
  )
}
