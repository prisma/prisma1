package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def mutaction: DatabaseMutaction
}
sealed trait FurtherNestedMutactionResult extends DatabaseMutactionResult {
  def id: IdGCValue
}

case class CreateNodeResult(id: IdGCValue, mutaction: CreateNode) extends FurtherNestedMutactionResult
case class UpdateNodeResult(id: IdGCValue, previousValues: PrismaNode, mutaction: UpdateNode) extends FurtherNestedMutactionResult {
  val namesOfUpdatedFields = mutaction.nonListArgs.keys ++ mutaction.listArgs.map(_._1)
}
case class DeleteNodeResult(id: IdGCValue, previousValues: PrismaNode, mutaction: DeleteNode) extends FurtherNestedMutactionResult
case class UpsertNodeResult(result: DatabaseMutaction, mutaction: UpsertNode)                 extends DatabaseMutactionResult
object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def mutaction: DatabaseMutaction = ???
}
