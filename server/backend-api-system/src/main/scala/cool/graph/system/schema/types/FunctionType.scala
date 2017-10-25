package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object FunctionType {
  lazy val Type = EnumType("FunctionType",
                           values = List(
                             EnumValue("WEBHOOK", value = models.FunctionType.WEBHOOK),
                             EnumValue("AUTH0", value = models.FunctionType.CODE)
                           ))
}
