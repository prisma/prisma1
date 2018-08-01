package com.prisma.api.mutations

import com.prisma.api.connector._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Project
import cool.graph.cuid.Cuid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ClientMutation[T] {
  val mutationId: Id = Cuid.createCuid()
  def dataResolver: DataResolver
  def prepareMutactions(): Future[TopLevelDatabaseMutaction]
  def getReturnValue(results: MutactionResults): Future[T]

  def projectId: String = dataResolver.project.id
  def project: Project
}

trait SingleItemClientMutation extends ClientMutation[ReturnValueResult] {
  def returnValueByUnique(where: NodeSelector, selectedFields: SelectedFields): Future[ReturnValueResult] = {
    dataResolver.getNodeByWhere(where, selectedFields).map {
      case Some(prismaNode) => ReturnValue(prismaNode)
      case None             => NoReturnValue(where)
    }
  }
}

case class PreparedMutactions(
    databaseMutactions: Vector[DatabaseMutaction], // DatabaseMutaction
    sideEffectMutactions: Vector[SideEffectMutaction] // SideEffectMutaction
) {
  lazy val allMutactions = databaseMutactions ++ sideEffectMutactions
}

sealed trait ReturnValueResult
case class BatchPayload(count: Long)
case class ReturnValue(prismaNode: PrismaNode) extends ReturnValueResult
case class NoReturnValue(where: NodeSelector)  extends ReturnValueResult
