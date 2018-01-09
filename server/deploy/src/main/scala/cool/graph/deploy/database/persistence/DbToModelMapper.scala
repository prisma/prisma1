package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Migration, Project}
import cool.graph.shared.models
import cool.graph.shared.models.{MigrationStep, Schema, Function}

object DbToModelMapper {
  import cool.graph.shared.models.MigrationStepsJsonFormatter._
  import cool.graph.shared.models.ProjectJsonFormatter._

//  def convert(migration: Migration): models.Project = {
//    val projectModel = migration.schema.as[models.Project]
//    projectModel.copy(revision = migration.revision)
//  }

//  def convert(project: Project, migration: Migration): models.Project = {
//    val projectModel = migration.schema.as[models.Project]
//    projectModel.copy(revision = migration.revision)
//  }

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
      errors = migration.errors.as[Vector[String]]
    )
  }
}
