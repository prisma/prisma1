package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models
import sangria.schema._

object ProjectType {
  lazy val Type: ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("name", StringType, resolve = _.value.projectId.name),
      Field("stage", StringType, resolve = _.value.projectId.stage)
    )
  )
}
