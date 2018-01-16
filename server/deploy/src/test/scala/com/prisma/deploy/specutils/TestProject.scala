package com.prisma.deploy.specutils

import cool.graph.cuid.Cuid
import com.prisma.shared.models.{Project, Schema}

object TestProject {
  def apply(): Project = {
    val projectId = Cuid.createCuid() + "@" + Cuid.createCuid()
    Project(id = projectId, ownerId = Cuid.createCuid(), schema = Schema())
  }
}
