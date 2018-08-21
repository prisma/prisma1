package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.deploy.connector.mongo.database.{MigrationDefinition, ProjectDefinition}
import com.prisma.shared.models
import com.prisma.shared.models.ProjectJsonFormatter._
import com.prisma.shared.models.MigrationStepsJsonFormatter._
import com.prisma.shared.models.{MigrationStatus, MigrationStep, Schema}
import play.api.libs.json.JsValue

object DbToModelMapper {

  def convert(project: ProjectDefinition, migration: MigrationDefinition): models.Project = {
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
    MissingBackRelations.add(schemaWithMissingBackRelations)
  }

  def convert(migration: MigrationDefinition): models.Migration = {
    val status = migration.status match {
      case "PENDING"          => MigrationStatus.Pending
      case "IN_PROGRESS"      => MigrationStatus.InProgress
      case "SUCCESS"          => MigrationStatus.Success
      case "ROLLING_BACK"     => MigrationStatus.RollingBack
      case "ROLLBACK_SUCCESS" => MigrationStatus.RollbackSuccess
      case "ROLLBACK_FAILURE" => MigrationStatus.RollbackFailure
    }

    models.Migration(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = migration.schema.as[Schema],
      functions = migration.functions.as[Vector[models.Function]],
      status = status,
      applied = migration.applied,
      rolledBack = migration.rolledBack,
      steps = migration.steps.as[Vector[MigrationStep]],
      errors = migration.errors.as[Vector[String]],
      startedAt = migration.startedAt,
      finishedAt = migration.finishedAt
    )
  }
}
