package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult
sealed trait FurtherNestedMutactionResult extends DatabaseMutactionResult {
  def id: IdGCValue
}

case class CreateNodeResult(id: IdGCValue, mutaction: CreateNode) extends FurtherNestedMutactionResult
case class UpdateNodeResult(id: IdGCValue, previousValues: PrismaNode, mutaction: UpdateNode) extends FurtherNestedMutactionResult {
  val namesOfUpdatedFields = mutaction.nonListArgs.keys ++ mutaction.listArgs.map(_._1)
}
case class DeleteDataItemResult(id: IdGCValue, previousValues: PrismaNode) extends FurtherNestedMutactionResult
case class UpsertDataItemResult(mutaction: DatabaseMutaction)              extends DatabaseMutactionResult
//sealed trait UpsertDataItemResult                                          extends FurtherNestedMutactionResult
//case class UpsertNodeCreated(result: CreateDataItemResult)                 extends UpsertDataItemResult { def id = result.id }
//case class UpsertNodeUpdated(result: UpdateItemResult)                     extends UpsertDataItemResult { def id = result.id }

object UnitDatabaseMutactionResult extends DatabaseMutactionResult
