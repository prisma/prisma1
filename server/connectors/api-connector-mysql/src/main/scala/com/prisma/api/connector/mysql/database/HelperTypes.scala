package com.prisma.api.connector.mysql.database

import com.prisma.gc_values.GCValue
import com.prisma.shared.models.IdType.Id

object HelperTypes {
  case class ScalarListElement(nodeId: Id, position: Int, value: GCValue)
}
