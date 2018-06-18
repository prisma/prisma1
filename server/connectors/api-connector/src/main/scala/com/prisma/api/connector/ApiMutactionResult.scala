package com.prisma.api.connector

import com.prisma.gc_values.{GCValue, IdGcValue}

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def id: IdGcValue
}
case class CreateDataItemResult(id: IdGcValue) extends DatabaseMutactionResult
object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def id = throw new NoSuchElementException("Unit results don't have ids")
}
