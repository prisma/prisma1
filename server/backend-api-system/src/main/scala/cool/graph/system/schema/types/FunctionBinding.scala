package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object FunctionBinding {
  lazy val Type = EnumType(
    "FunctionBinding",
    values = List(
      EnumValue("TRANSFORM_ARGUMENT", value = models.FunctionBinding.TRANSFORM_ARGUMENT),
      EnumValue("PRE_WRITE", value = models.FunctionBinding.PRE_WRITE),
      EnumValue("TRANSFORM_PAYLOAD", value = models.FunctionBinding.TRANSFORM_PAYLOAD)
    )
  )
}
