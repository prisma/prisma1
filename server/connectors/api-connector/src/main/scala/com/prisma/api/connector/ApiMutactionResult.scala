package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult
sealed trait FurtherNestedMutactionResult extends DatabaseMutactionResult {
  def id: IdGCValue
}

case class CreateDataItemResult(id: IdGCValue) extends FurtherNestedMutactionResult
case class UpdateItemResult(id: IdGCValue)     extends FurtherNestedMutactionResult

object UnitDatabaseMutactionResult extends DatabaseMutactionResult
/**
  * Subscriptions:
  *
  * - Create
  * - NestedCreateDataItem
  * - UpdateDataItem
  * - DeleteDataItem
  * - UpsertDataItem
  */
