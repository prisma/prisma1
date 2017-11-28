package cool.graph.api.schema

import cool.graph.shared.models
import sangria.schema._

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
