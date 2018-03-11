package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models
import com.prisma.shared.models.Project
import sangria.schema._

object ProjectType {
  lazy val Type: ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("metricKey", StringType, resolve = ctx => metricKey(ctx.value)),
      Field("name", StringType, resolve = _.value.projectId.name),
      Field("stage", StringType, resolve = _.value.projectId.stage)
    )
  )

  def metricKey(project: Project): String = project.id.replace('@', '-').replace('~', '-')
}
