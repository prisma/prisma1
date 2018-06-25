package com.prisma.api.mutations

import com.prisma.api.connector.{DatabaseMutactionExecutor, DatabaseMutactionResult, ServerSideSubscription}
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, ServerSideSubscriptions, SideEffectMutactionExecutor}

import scala.concurrent.{ExecutionContext, Future}

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor,
      databaseMutactionVerifier: DatabaseMutactionVerifier
  )(implicit ec: ExecutionContext): Future[T] = {
    for {
      mutaction               <- clientMutation.prepareMutactions()
      errors                  = databaseMutactionVerifier.verify(mutaction +: mutaction.allMutactions)
      _                       = if (errors.nonEmpty) throw errors.head
      databaseResults         <- databaseMutactionExecutor.executeTransactionally(mutaction)
      serverSideSubscriptions = ServerSideSubscriptions.extractFromMutactions(clientMutation.project, databaseResults, requestId = "")
      // fixme: also add PublishSubscriptionEvents
      _          <- sideEffectMutactionExecutor.execute(serverSideSubscriptions)
      prismaNode <- clientMutation.getReturnValue(databaseResults)
    } yield prismaNode
  }

//  private def performMutactions(
//      preparedMutactions: PreparedMutactions,
//      projectId: String,
//      databaseMutactionExecutor: DatabaseMutactionExecutor,
//      sideEffectMutactionExecutor: SideEffectMutactionExecutor
//  )(implicit ec: ExecutionContext): Future[Vector[DatabaseMutactionResult]] = {
//    for {
//      databaseResults <- databaseMutactionExecutor.execute(preparedMutactions.databaseMutactions)
//      _               <- sideEffectMutactionExecutor.execute(preparedMutactions.sideEffectMutactions)
//    } yield databaseResults
//  }
}
