package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.{CustomScalarTypes, SystemUserContext}
import com.prisma.shared.models
import sangria.schema._

object MigrationType {
  lazy val Type: ObjectType[SystemUserContext, models.Migration] = ObjectType(
    "Migration",
    "This is a migration",
    fields[SystemUserContext, models.Migration](
      Field("projectId", StringType, resolve = _.value.projectId),
      Field("revision", IntType, resolve = _.value.revision),
      Field("status", StringType, resolve = _.value.status.toString),
      Field("applied", IntType, resolve = _.value.applied),
      Field("rolledBack", IntType, resolve = _.value.rolledBack),
      Field("steps", ListType(MigrationStepType.Type), resolve = _.value.steps),
      Field("errors", ListType(StringType), resolve = _.value.errors),
      Field("startedAt", OptionType(CustomScalarTypes.DateTimeType), resolve = _.value.startedAt),
      Field("finishedAt", OptionType(CustomScalarTypes.DateTimeType), resolve = _.value.finishedAt)
    )
  )
}
