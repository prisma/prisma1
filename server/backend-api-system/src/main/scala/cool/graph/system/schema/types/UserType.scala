package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object UserType {
  val Type = EnumType("UserType",
                      values = List(EnumValue("EVERYONE", value = models.UserType.Everyone), EnumValue("AUTHENTICATED", value = models.UserType.Authenticated)))
}
