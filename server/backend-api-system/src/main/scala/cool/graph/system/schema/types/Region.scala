package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object Region {
  lazy val Type = EnumType(
    "Region",
    values = List(
      EnumValue("EU_WEST_1", value = models.Region.EU_WEST_1),
      EnumValue("AP_NORTHEAST_1", value = models.Region.AP_NORTHEAST_1),
      EnumValue("US_WEST_2", value = models.Region.US_WEST_2)
    )
  )
}
