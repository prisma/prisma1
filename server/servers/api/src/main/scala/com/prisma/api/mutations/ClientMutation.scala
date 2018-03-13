package com.prisma.api.mutations

import com.prisma.api.connector.NodeSelector
import com.prisma.api.database.mutactions._
import com.prisma.api.database.{DataItem, DataResolver}
import cool.graph.cuid.Cuid
import com.prisma.shared.models.IdType.Id

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ClientMutation[T] {
  val mutationId: Id = Cuid.createCuid()
  def dataResolver: DataResolver
  def prepareMutactions(): Future[PreparedMutactions]
  def getReturnValue: Future[T]
}

trait SingleItemClientMutation extends ClientMutation[ReturnValueResult] {
  def returnValueByUnique(where: NodeSelector): Future[ReturnValueResult] = {
    dataResolver.resolveByUnique(where).map {
      case Some(dataItem) => ReturnValue(dataItem)
      case None           => NoReturnValue(where)
    }
  }
}

case class PreparedMutactions(
    databaseMutactions: Vector[ClientSqlMutaction], // DatabaseMutaction
    sideEffectMutactions: Vector[Mutaction] // SideEffectMutaction
) {
  lazy val allMutactions = databaseMutactions ++ sideEffectMutactions
}

sealed trait ReturnValueResult
case class BatchPayload(count: Long)
case class ReturnValue(dataItem: DataItem)    extends ReturnValueResult
case class NoReturnValue(where: NodeSelector) extends ReturnValueResult
