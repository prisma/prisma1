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
case class ManyNodesResult(mutaction: FinalMutaction, count: Int)                             extends DatabaseMutactionResult

object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def mutaction: DatabaseMutaction = ???
}

object ManyHelper {
  def getManyCount(result: MutactionResults): Int = result.results match {
    case Vector.empty => sys.error("ManyMutation should always return a ManyNodesResult")
    case x =>
      x.head match {
        case ManyNodesResult(_, count) => count
        case _                         => sys.error("ManyMutation should always return a ManyNodesResult")
      }
  }

}
