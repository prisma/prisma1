package com.prisma.shared.schema_dsl

import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.TestIds._

object TestProject {
  val empty = this.apply()

  def apply(): Project = {
    Project(id = testProjectId, schema = Schema())
  }
}
