package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import sangria.schema._

object ProjectType {

  def Type(encoder: ProjectIdEncoder): ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("metricKey", StringType, resolve = ctx => metricKey(ctx.value, encoder)),
      Field("name", StringType, resolve = x => encoder.fromEncodedString(x.value.id).name),
      Field("stage", StringType, resolve = x => encoder.fromEncodedString(x.value.id).stage)
    )
  )

  def metricKey(project: Project, encoder: ProjectIdEncoder): String = project.id.replace(encoder.stageSeparator, '-').replace(encoder.workspaceSeparator, '-')
}
