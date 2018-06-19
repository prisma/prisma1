package com.prisma.deploy.schema.types

import com.prisma.deploy.migration.validation.DeployWarning
import com.prisma.deploy.schema.SystemUserContext
import sangria.schema._

object DeployWarningType {
  lazy val TheListType = ListType(Type)

  lazy val Type: ObjectType[SystemUserContext, DeployWarning] = ObjectType(
    "SchemaWarning",
    "A warning created while validating the schema against existing data.",
    List.empty,
    fields[SystemUserContext, DeployWarning](
      Field("type", StringType, resolve = _.value.`type`),
      Field("field", OptionType(StringType), resolve = _.value.field),
      Field("description", StringType, resolve = _.value.description)
    )
  )
}
