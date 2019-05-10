package com.prisma.deploy.schema.types

import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models
import com.prisma.shared.models.{MigrationId, ProjectIdEncoder}
import sangria.schema._

import scala.concurrent.ExecutionContext

object ProjectType {

  def Type(
      encoder: ProjectIdEncoder,
      migrationPersistence: MigrationPersistence
  )(implicit ec: ExecutionContext): ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    fields[SystemUserContext, models.Project](
      Field("name", StringType, resolve = x => encoder.fromEncodedString(x.value.id).name),
      Field("stage", StringType, resolve = x => encoder.fromEncodedString(x.value.id).stage),
      Field("datamodel", StringType, resolve = { ctx =>
        val project = ctx.value
        migrationPersistence.byId(MigrationId(project.id, project.revision)).map(_.get.rawDataModel)
      })
    )
  )
}
