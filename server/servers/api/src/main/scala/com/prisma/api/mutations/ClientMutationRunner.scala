package com.prisma.api.mutations

import com.prisma.api.connector.{DatabaseMutactionExecutor, DatabaseMutactionResult, ExecuteServerSideSubscription}
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
      errors                  = databaseMutactionVerifier.verify(mutaction +: mutaction.allNestedMutactions)
      _                       = if (errors.nonEmpty) throw errors.head
      databaseResults         <- databaseMutactionExecutor.executeTransactionally(mutaction)
      serverSideSubscriptions = ServerSideSubscriptions.extractFromMutactions(clientMutation.project, databaseResults, requestId = "")
      // fixme: also add PublishSubscriptionEvents
      _          <- sideEffectMutactionExecutor.execute(serverSideSubscriptions)
      prismaNode <- clientMutation.getReturnValue(databaseResults)
    } yield prismaNode
  }
}
