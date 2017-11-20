package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Project
import cool.graph.shared.models
import play.api.libs.json.{JsObject, Json}

object ModelToDbMapper {
  import ProjectJsonFormatter._

  def convert(project: models.Project): Project = {
    val modelJson = Json.toJson(project)
    Project(
      id = project.id,
      alias = project.alias,
      name = project.name,
      revision = project.revision,
      clientId = project.ownerId,
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      model = modelJson,
      migrationSteps = JsObject.empty
    )
  }
}
