package cool.graph.client.schema

import sangria.schema._

import cool.graph.shared.models

object ModelMutationType {
  val Type = EnumType(
    "_ModelMutationType",
    values = List(
      EnumValue("CREATED", value = models.ModelMutationType.Created),
      EnumValue("UPDATED", value = models.ModelMutationType.Updated),
      EnumValue("DELETED", value = models.ModelMutationType.Deleted)
    )
  )
}
