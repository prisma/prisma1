package com.prisma.deploy.database.persistence

import com.prisma.deploy.database.tables.{Migration, Project}
import com.prisma.shared.models
import com.prisma.shared.models.{MigrationStep, Schema}

object DbToModelMapper {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  def convert(project: Project, migration: Migration): models.Project = {
    models.Project(
      id = project.id,
      ownerId = project.ownerId.getOrElse(""),
      revision = migration.revision,
      schema = migration.schema.as[Schema],
      webhookUrl = project.webhookUrl,
      secrets = project.secrets.as[Vector[String]],
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      functions = migration.functions.as[List[models.Function]]
    )
  }

  def convert(migration: Migration): models.Migration = {
    models.Migration(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = migration.schema.as[Schema],
      functions = migration.functions.as[Vector[models.Function]],
      status = migration.status,
      applied = migration.applied,
      rolledBack = migration.rolledBack,
      steps = migration.steps.as[Vector[MigrationStep]],
      errors = migration.errors.as[Vector[String]],
      startedAt = migration.startedAt,
      finishedAt = migration.finishedAt
    )
  }
}
