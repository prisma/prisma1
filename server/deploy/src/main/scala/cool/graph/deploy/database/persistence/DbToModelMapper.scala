package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Project
import cool.graph.shared.models

object DbToModelMapper {
  import ProjectJsonFormatter._

  def convert(project: Project): models.Project = {
    val projectModel = project.model.as[models.Project]
    projectModel.copy(revision = project.revision)
  }
}
