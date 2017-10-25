package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object LogStatus {
  lazy val Type = EnumType("LogStatus",
                           values = List(
                             EnumValue("SUCCESS", value = models.LogStatus.SUCCESS),
                             EnumValue("FAILURE", value = models.LogStatus.FAILURE)
                           ))
}
