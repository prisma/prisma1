package cool.graph.system.schema.types

import cool.graph.system.SystemUserContext
import cool.graph.system.migration.dataSchema.{VerbalDescription, VerbalSubDescription}
import sangria.schema._

object VerbalDescriptionType {
  lazy val TheListType = ListType(Type)

  lazy val Type: ObjectType[SystemUserContext, VerbalDescription] = ObjectType(
    "MigrationMessage",
    "verbal descriptions of actions taken during a schema migration",
    List.empty,
    fields[SystemUserContext, VerbalDescription](
      Field("type", StringType, resolve = _.value.`type`),
      Field("action", StringType, resolve = _.value.action),
      Field("name", StringType, resolve = _.value.name),
      Field("description", StringType, resolve = _.value.description),
      Field("subDescriptions", ListType(SubDescriptionType), resolve = _.value.subDescriptions)
    )
  )

  lazy val SubDescriptionType: ObjectType[SystemUserContext, VerbalSubDescription] = ObjectType(
    "MigrationSubMessage",
    "verbal descriptions of actions taken during a schema migration",
    List.empty,
    fields[SystemUserContext, VerbalSubDescription](
      Field("type", StringType, resolve = _.value.`type`),
      Field("action", StringType, resolve = _.value.action),
      Field("name", StringType, resolve = _.value.name),
      Field("description", StringType, resolve = _.value.description)
    )
  )
}
