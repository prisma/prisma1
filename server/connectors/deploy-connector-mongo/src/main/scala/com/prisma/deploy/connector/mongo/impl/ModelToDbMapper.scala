package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.mongo.database.{MigrationDefinition, ProjectDefinition}
import com.prisma.shared.models
import play.api.libs.json.Json

object ModelToDbMapper {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  def convert(project: models.Project): ProjectDefinition = {
    val secretsJson   = Json.toJson(project.secrets)
    val functionsJson = Json.toJson(project.functions)

    ProjectDefinition(
      id = project.id,
      ownerId = Some(project.ownerId),
      project.webhookUrl,
      secretsJson,
      project.allowQueries,
      project.allowMutations,
      functionsJson
    )
  }

  def convert(migration: models.Migration): MigrationDefinition = {
    val schemaJson         = Json.toJson(migration.schema)
    val functionsJson      = Json.toJson(migration.functions)
    val migrationStepsJson = Json.toJson(migration.steps)
    val errorsJson         = Json.toJson(migration.errors)

    MigrationDefinition(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = schemaJson,
      functions = functionsJson,
      status = migration.status,
      applied = migration.applied,
      rolledBack = migration.rolledBack,
      steps = migrationStepsJson,
      errors = errorsJson,
      startedAt = migration.startedAt,
      finishedAt = migration.finishedAt
    )
  }
}
