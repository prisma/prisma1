package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult {
  def mutaction: DatabaseMutaction
  def merge(otherResults: MutactionResults): MutactionResults         = MutactionResults(Vector(this)).merge(otherResults)
  def merge(otherResults: Vector[MutactionResults]): MutactionResults = MutactionResults(Vector(this)).merge(otherResults)
}

sealed trait FurtherNestedMutactionResult extends DatabaseMutactionResult {
  def id: IdGCValue
  def nodeAddress: NodeAddress
}

object CreateNodeResult {
  def apply(id: IdGCValue, mutaction: CreateNode): CreateNodeResult =
    CreateNodeResult(NodeAddress.forId(mutaction.model, id), mutaction)
}

case class CreateNodeResult(nodeAddress: NodeAddress, mutaction: CreateNode) extends FurtherNestedMutactionResult {
  val id: IdGCValue = nodeAddress.where.fieldGCValue.asInstanceOf[IdGCValue] //This always returns the toplevel id, even for embedded types
}

object UpdateNodeResult {
  def apply(id: IdGCValue, previousValues: PrismaNode, mutaction: UpdateNode): UpdateNodeResult =
    UpdateNodeResult(NodeAddress.forId(mutaction.model, id), previousValues, mutaction)
}

case class UpdateNodeResult(nodeAddress: NodeAddress, previousValues: PrismaNode, mutaction: UpdateNode) extends FurtherNestedMutactionResult {
  val namesOfUpdatedFields = mutaction.nonListArgs.keys ++ mutaction.listArgs.map(_._1)
  val id: IdGCValue        = nodeAddress.where.fieldGCValue.asInstanceOf[IdGCValue]
}

case class DeleteNodeResult(previousValues: PrismaNode, mutaction: DeleteNode) extends DatabaseMutactionResult
case class UpsertNodeResult(result: DatabaseMutaction, mutaction: UpsertNode)  extends DatabaseMutactionResult
case class ManyNodesResult(mutaction: FinalMutaction, count: Int)              extends DatabaseMutactionResult

object UnitDatabaseMutactionResult extends DatabaseMutactionResult {
  override def mutaction: DatabaseMutaction = ???
}

object ManyHelper {
  def getManyCount(result: MutactionResults): Int = result.results.headOption match {
    case Some(ManyNodesResult(_, count)) => count
    case _                               => sys.error("ManyMutation should always return a ManyNodesResult")
  }
}
