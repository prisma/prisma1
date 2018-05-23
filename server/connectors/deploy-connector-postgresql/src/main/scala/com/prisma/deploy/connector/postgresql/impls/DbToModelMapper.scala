package com.prisma.deploy.connector.postgresql.impls

import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.deploy.connector.postgresql.database.{Migration, ProjectDefinition}
import com.prisma.shared.models
import com.prisma.shared.models.MigrationStepsJsonFormatter._
import com.prisma.shared.models.ProjectJsonFormatter._
import com.prisma.shared.models.{MigrationStep, Schema}
import play.api.libs.json.JsValue

object DbToModelMapper {

  def convert(project: ProjectDefinition, migration: Migration): models.Project = {
    models.Project(
      id = project.id,
      ownerId = project.ownerId.getOrElse(""),
      revision = migration.revision,
      schema = convertSchema(migration.schema),
      webhookUrl = project.webhookUrl,
      secrets = project.secrets.as[Vector[String]],
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      functions = migration.functions.as[List[models.Function]]
    )
  }

  private def convertSchema(schema: JsValue): Schema = {
    val schemaWithMissingBackRelations = schema.as[Schema]
    MissingBackRelations.addMissingBackRelations(schemaWithMissingBackRelations)
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
