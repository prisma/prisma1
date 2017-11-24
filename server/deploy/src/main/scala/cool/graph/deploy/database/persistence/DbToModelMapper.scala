package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Client, Project}
import cool.graph.shared.models

object DbToModelMapper {
  import ProjectJsonFormatter._

  def convert(project: Project): models.Project = {
    val projectModel = project.model.as[models.Project]
    projectModel.copy(revision = project.revision)
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
