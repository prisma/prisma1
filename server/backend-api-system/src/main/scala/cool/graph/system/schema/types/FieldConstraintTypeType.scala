package cool.graph.system.schema.types

import sangria.schema.{EnumType, EnumValue}

object FieldConstraintTypeType {
  val enum = cool.graph.shared.models.FieldConstraintType

  lazy val Type = EnumType(
    "FieldConstraintTypeType",
    values = List(
      EnumValue("STRING", value = enum.STRING),
      EnumValue("NUMBER", value = enum.NUMBER),
      EnumValue("BOOLEAN", value = enum.BOOLEAN),
      EnumValue("LIST", value = enum.LIST)
    )
  )
}
