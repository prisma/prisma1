package com.prisma.api.connector

import com.prisma.gc_values.GCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def id: GCValue
}
case class CreateDataItemResult(id: GCValue) extends DatabaseMutactionResult
object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def id = throw new NoSuchElementException("Unit results don't have ids")
}
