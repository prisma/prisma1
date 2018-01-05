package cool.graph.deploy.schema.types

import cool.graph.deploy.schema.SystemUserContext
import cool.graph.shared.models
import sangria.schema._

object MigrationType {
  lazy val Type: ObjectType[SystemUserContext, models.Migration] = ObjectType(
    "Migration",
    "This is a migration",
    fields[SystemUserContext, models.Migration](
      Field("projectId", StringType, resolve = _.value.projectId),
      Field("revision", IntType, resolve = _.value.revision),
      Field("status", StringType, resolve = _.value.status.toString),
      Field("progress", StringType, resolve = x => s"${x.value.progress}/${x.value.steps.length}"),
      Field("steps", ListType(MigrationStepType.Type), resolve = _.value.steps),
      Field("errors", ListType(StringType), resolve = _.value.errors)
    )
  )
}
