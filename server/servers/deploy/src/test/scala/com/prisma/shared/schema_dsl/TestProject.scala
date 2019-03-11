package com.prisma.shared.schema_dsl

import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.TestIds._

object TestProject {
  val empty    = this.apply()
  val emptyV11 = this.applyv2()

  def apply(): Project = {
    Project(id = testProjectId, schema = Schema.empty)
  }

  def applyv2(): Project = {
    Project(id = testProjectId, schema = Schema.emptyV11)
  }
}
