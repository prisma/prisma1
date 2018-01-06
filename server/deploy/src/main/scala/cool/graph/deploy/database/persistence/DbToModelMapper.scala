package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Migration, Project}
import cool.graph.shared.models
import cool.graph.shared.models.{MigrationStep, Schema}

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
      project.id,
      project.ownerId.getOrElse(""),
      migration.revision,
      migration.schema.as[Schema],
      project.webhookUrl,
      project.secrets.as[Vector[String]],
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      project.functions.as[List[models.Function]]
    )
  }

  def convert(migration: Migration): models.Migration = {
    models.Migration(
      migration.projectId,
      migration.revision,
      migration.schema.as[Schema],
      migration.status,
      migration.applied,
      migration.rolledBack,
      migration.steps.as[Vector[MigrationStep]],
      migration.errors.as[Vector[String]]
    )
  }
}
