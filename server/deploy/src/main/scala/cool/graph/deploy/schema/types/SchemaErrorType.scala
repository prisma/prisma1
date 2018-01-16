package cool.graph.deploy.schema.types

import cool.graph.deploy.migration.validation.SchemaError
import cool.graph.deploy.schema.SystemUserContext
import sangria.schema._

object SchemaErrorType {
  lazy val TheListType = ListType(Type)

  lazy val Type: ObjectType[SystemUserContext, SchemaError] = ObjectType(
    "SchemaError",
    "An error that occurred while validating the schema.",
    List.empty,
    fields[SystemUserContext, SchemaError](
      Field("type", StringType, resolve = _.value.`type`),
      Field("field", OptionType(StringType), resolve = _.value.field),
      Field("description", StringType, resolve = _.value.description)
    )
  )
}
