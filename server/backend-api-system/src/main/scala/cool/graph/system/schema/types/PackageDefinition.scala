package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema._

object PackageDefinition {
  lazy val Type: ObjectType[SystemUserContext, models.PackageDefinition] = ObjectType(
    "PackageDefinition",
    "this is a beta feature. Expect breaking changes.",
    interfaces[SystemUserContext, models.PackageDefinition](nodeInterface),
    idField[SystemUserContext, models.PackageDefinition] ::
      fields[SystemUserContext, models.PackageDefinition](
      Field("definition", StringType, resolve = _.value.definition),
      Field("name", OptionType(StringType), resolve = _.value.name)
    )
  )
}
