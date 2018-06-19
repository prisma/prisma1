package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def id: Option[IdGCValue]
}
case class CreateDataItemResult(createdId: IdGCValue) extends DatabaseMutactionResult {
  override def id = Some(createdId)
}
case class UpdateItemResult(id: Option[IdGCValue]) extends DatabaseMutactionResult
object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def id = None
}
