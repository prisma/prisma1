package com.prisma.api.schema

import com.prisma.shared.models
import sangria.schema._

object ModelMutationType {
  val Type = EnumType(
    "MutationType",
    values = List(
      EnumValue("CREATED", value = models.ModelMutationType.Created),
      EnumValue("UPDATED", value = models.ModelMutationType.Updated),
      EnumValue("DELETED", value = models.ModelMutationType.Deleted)
    )
  )
}
