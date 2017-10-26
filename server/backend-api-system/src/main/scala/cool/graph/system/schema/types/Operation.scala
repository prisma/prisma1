package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object Operation {
  val Type = EnumType(
    "Operation",
    values = List(
      EnumValue("READ", value = models.ModelOperation.Read),
      EnumValue("CREATE", value = models.ModelOperation.Create),
      EnumValue("UPDATE", value = models.ModelOperation.Update),
      EnumValue("DELETE", value = models.ModelOperation.Delete)
    )
  )
}
