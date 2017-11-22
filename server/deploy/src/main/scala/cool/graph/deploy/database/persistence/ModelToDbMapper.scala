package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{Client, Project}
import cool.graph.shared.models
import play.api.libs.json.{JsObject, Json}

object ModelToDbMapper {
  import ProjectJsonFormatter._

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
    val modelJson = Json.toJson(project)
    Project(
      id = project.id,
      alias = project.alias,
      name = project.name,
      revision = project.revision,
      clientId = project.ownerId,
      model = modelJson,
      migrationSteps = JsObject.empty
    )
  }
}
