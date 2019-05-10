package com.prisma.shared.schema_dsl

import com.prisma.shared.models._

object TestProject {
  val emptyV11 = Project(id = SchemaDsl.testProjectId, schema = Schema.emptyV11)
}
