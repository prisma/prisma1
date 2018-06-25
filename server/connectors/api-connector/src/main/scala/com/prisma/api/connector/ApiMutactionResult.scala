package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult
sealed trait FurtherNestedMutactionResult extends DatabaseMutactionResult {
  def id: IdGCValue
}

case class CreateDataItemResult(id: IdGCValue)                             extends FurtherNestedMutactionResult
case class UpdateItemResult(id: IdGCValue)                                 extends FurtherNestedMutactionResult
case class DeleteDataItemResult(id: IdGCValue, previousValues: PrismaNode) extends FurtherNestedMutactionResult
sealed trait UpsertDataItemResult                                          extends FurtherNestedMutactionResult
case class UpsertNodeCreated(result: CreateDataItemResult)                 extends UpsertDataItemResult { def id = result.id }
case class UpsertNodeUpdated(result: UpdateItemResult)                     extends UpsertDataItemResult { def id = result.id }

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
