package com.prisma.api.connector

import com.prisma.gc_values.IdGcValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def id: Option[IdGcValue]
}
case class CreateDataItemResult(createdId: IdGcValue) extends DatabaseMutactionResult {
  override def id = Some(createdId)
}
case class UpdateItemResult(id: Option[IdGcValue]) extends DatabaseMutactionResult
object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def id = None
}
