package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Migration, Project}
import cool.graph.shared.models
import play.api.libs.json.Json

object ModelToDbMapper {
  import MigrationStepsJsonFormatter._
  import cool.graph.shared.models.ProjectJsonFormatter._

  def convert(project: models.Project): Project = {
    Project(
      id = project.id,
      alias = project.alias,
      name = project.name,
      ownerId = project.ownerId
    )
  }

  def convert(project: models.Project, migration: models.Migration): Migration = {
    val schemaJson         = Json.toJson(project)
    val migrationStepsJson = Json.toJson(migration.steps)

    Migration(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = schemaJson,
      steps = migrationStepsJson,
      hasBeenApplied = migration.hasBeenApplied
    )
  }
}
