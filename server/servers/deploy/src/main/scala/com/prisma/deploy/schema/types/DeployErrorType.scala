package com.prisma.deploy.schema.types

import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.schema.SystemUserContext
import sangria.schema._

object DeployErrorType {
  lazy val TheListType = ListType(Type)

  lazy val Type: ObjectType[SystemUserContext, DeployError] = ObjectType(
    "SchemaError",
    "An error that occurred while validating the schema.",
    List.empty,
    fields[SystemUserContext, DeployError](
      Field("type", StringType, resolve = _.value.`type`),
      Field("field", OptionType(StringType), resolve = _.value.field),
      Field("description", StringType, resolve = _.value.description)
    )
  )
}
