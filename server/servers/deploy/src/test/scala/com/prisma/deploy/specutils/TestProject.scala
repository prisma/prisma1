package com.prisma.deploy.specutils

import com.prisma.shared.models.{Project, Schema}
import cool.graph.cuid.Cuid

object TestProject {
  def apply(): Project = {
    val projectId = Cuid.createCuid() + "@" + Cuid.createCuid()
    Project(id = projectId, schema = Schema())
  }
}
