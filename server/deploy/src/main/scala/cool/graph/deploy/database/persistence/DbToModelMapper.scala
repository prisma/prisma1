package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Client, Migration, Project}
import cool.graph.shared.models
import cool.graph.shared.models.MigrationStep

object DbToModelMapper {
  import cool.graph.shared.models.ProjectJsonFormatter._
  import MigrationStepsJsonFormatter._

  def convert(project: Project, migration: Migration): models.Project = {
    val projectModel = migration.schema.as[models.Project]
    projectModel.copy(revision = migration.revision)
  }

  def convert(migration: Migration): models.Migration = {
    models.Migration(
      migration.projectId,
      migration.revision,
      migration.hasBeenApplied,
      migration.steps.as[Vector[MigrationStep]]
    )
  }

  def convert(client: Client): models.Client = {
    models.Client(
      id = client.id,
      auth0Id = client.auth0Id,
      isAuth0IdentityProviderEmail = client.isAuth0IdentityProviderEmail,
      name = client.name,
      email = client.email,
      hashedPassword = client.password,
      resetPasswordSecret = client.resetPasswordToken,
      source = client.source,
      projects = List.empty,
      createdAt = client.createdAt,
      updatedAt = client.updatedAt
    )
  }
}
