package cool.graph.deploy.specutils

import cool.graph.cuid.Cuid
import cool.graph.shared.models.Project

object TestProject {
  def apply(): Project = {
    val projectId = Cuid.createCuid() + "@" + Cuid.createCuid()
    Project(id = projectId, ownerId = Cuid.createCuid())
  }
}
