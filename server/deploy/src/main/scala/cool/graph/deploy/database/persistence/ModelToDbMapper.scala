package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Client, Migration, Project}
import cool.graph.shared.models
import play.api.libs.json.Json

object ModelToDbMapper {
  import MigrationStepsJsonFormatter._
  import cool.graph.shared.models.ProjectJsonFormatter._

  def convert(client: models.Client): Client = {
    Client(
      id = client.id,
      auth0Id = client.auth0Id,
      isAuth0IdentityProviderEmail = client.isAuth0IdentityProviderEmail,
      name = client.name,
      email = client.email,
      password = client.hashedPassword,
      resetPasswordToken = client.resetPasswordSecret,
      source = client.source,
      createdAt = client.createdAt,
      updatedAt = client.updatedAt
    )
  }

  def convert(project: models.Project): Project = {
    Project(
      id = project.id,
      clientId = project.ownerId
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
