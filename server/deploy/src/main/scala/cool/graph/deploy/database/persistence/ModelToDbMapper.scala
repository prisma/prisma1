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
      ownerId = Some(project.ownerId) // todo ideally, owner id is not optional or it is optional on models.Project as well
    )
  }

  def convert(project: models.Project, migration: models.Migration): Migration = {
    val schemaJson         = Json.toJson(project)
    val migrationStepsJson = Json.toJson(migration.steps)
    val errorsJson         = Json.toJson(migration.errors)

    Migration(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = schemaJson,
      status = migration.status,
      progress = migration.progress,
      steps = migrationStepsJson,
      errors = errorsJson
    )
  }
}
