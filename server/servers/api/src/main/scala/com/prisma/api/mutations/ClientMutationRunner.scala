package com.prisma.api.mutations

import com.prisma.api.connector.{DatabaseMutactionExecutor, DatabaseMutactionResult, MutationResult}
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, SideEffectMutactionExecutor}

import scala.concurrent.{ExecutionContext, Future}

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor,
      databaseMutactionVerifier: DatabaseMutactionVerifier
  )(implicit ec: ExecutionContext): Future[T] = {
    for {
      preparedMutactions <- clientMutation.prepareMutactions()
      //errors             = databaseMutactionVerifier.verify(preparedMutactions.databaseMutactions) // fixme: bring this back
//      _               = if (errors.nonEmpty) throw errors.head
      mutationResult <- performMutactions(preparedMutactions, clientMutation.projectId, databaseMutactionExecutor, sideEffectMutactionExecutor)
      prismaNode     <- clientMutation.getReturnValue(mutationResult)
    } yield prismaNode
  }

  private def performMutactions(
      preparedMutactions: PreparedMutactions,
      projectId: String,
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor
  )(implicit ec: ExecutionContext): Future[MutationResult] = {
    for {
      databaseResults <- databaseMutactionExecutor.execute(preparedMutactions.mutation)
      _               <- sideEffectMutactionExecutor.execute(preparedMutactions.sideEffectMutactions)
    } yield databaseResults
  }
}
