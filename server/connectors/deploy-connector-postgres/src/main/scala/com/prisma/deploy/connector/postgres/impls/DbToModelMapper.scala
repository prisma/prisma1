package com.prisma.deploy.connector.postgres.impls

import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.deploy.connector.postgres.database.Migration
import com.prisma.shared.models
import com.prisma.shared.models.MigrationStepsJsonFormatter._
import com.prisma.shared.models.ProjectJsonFormatter._
import com.prisma.shared.models.{MigrationStep, Schema}
import play.api.libs.json.JsValue

object DbToModelMapper {
  private def convertSchema(schema: JsValue): Schema = {
    val schemaWithMissingBackRelations = schema.as[Schema]
    MissingBackRelations.add(schemaWithMissingBackRelations)
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
