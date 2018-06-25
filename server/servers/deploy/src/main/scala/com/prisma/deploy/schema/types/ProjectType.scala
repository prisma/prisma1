package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models
import com.prisma.shared.models.ProjectIdEncoder
import sangria.schema._

object ProjectType {

  def Type(encoder: ProjectIdEncoder): ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("name", StringType, resolve = x => encoder.fromEncodedString(x.value.id).name),
      Field("stage", StringType, resolve = x => encoder.fromEncodedString(x.value.id).stage)
    )
  )
}
