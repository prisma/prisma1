package cool.graph.deploy.schema.types

import cool.graph.deploy.schema.SystemUserContext
import cool.graph.shared.models
import sangria.schema._

object ProjectType {
  lazy val Type: ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("alias", OptionType(StringType), resolve = _.value.alias),
      Field("revision", OptionType(IntType), resolve = _.value.revision)
    )
  )
}
