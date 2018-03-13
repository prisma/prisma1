package com.prisma.deploy.schema.types

import com.prisma.deploy.migration.validation.SchemaWarning
import com.prisma.deploy.schema.SystemUserContext
import sangria.schema._

object SchemaWarningType {
  lazy val TheListType = ListType(Type)

  lazy val Type: ObjectType[SystemUserContext, SchemaWarning] = ObjectType(
    "SchemaWarning",
    "A warning created while validating the schema.",
    List.empty,
    fields[SystemUserContext, SchemaWarning](
      Field("type", StringType, resolve = _.value.`type`),
      Field("field", OptionType(StringType), resolve = _.value.field),
      Field("description", StringType, resolve = _.value.description)
    )
  )
}
