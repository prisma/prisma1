package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.mysql.database.{Migration, Project}
import com.prisma.shared.models
import play.api.libs.json.Json

object ModelToDbMapper {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  def convert(project: models.Project): Project = {
    val secretsJson   = Json.toJson(project.secrets)
    val functionsJson = Json.toJson(project.functions)

    Project(
      id = project.id,
      ownerId = Some(project.ownerId), // todo ideally, owner id is not optional or it is optional on models.Project as well
      project.webhookUrl,
      secretsJson,
      project.allowQueries,
      project.allowMutations,
      functionsJson
    )
  }

  def convert(migration: models.Migration): Migration = {
    val schemaJson         = Json.toJson(migration.schema)
    val functionsJson      = Json.toJson(migration.functions)
    val migrationStepsJson = Json.toJson(migration.steps)
    val errorsJson         = Json.toJson(migration.errors)

    Migration(
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
