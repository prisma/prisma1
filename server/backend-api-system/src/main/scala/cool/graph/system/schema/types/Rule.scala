package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object Rule {
  val Type = EnumType(
    "Rule",
    values = List(EnumValue("NONE", value = models.CustomRule.None),
                  EnumValue("GRAPH", value = models.CustomRule.Graph),
                  EnumValue("WEBHOOK", value = models.CustomRule.Webhook))
  )
}
