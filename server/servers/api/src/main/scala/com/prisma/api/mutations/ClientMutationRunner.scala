package com.prisma.api.mutations

import com.prisma.api.connector.DatabaseMutactionExecutor
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, ServerSideSubscriptions, SideEffectMutactionExecutor, SubscriptionEvents}

import scala.concurrent.{ExecutionContext, Future}

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor,
      databaseMutactionVerifier: DatabaseMutactionVerifier
  )(implicit ec: ExecutionContext): Future[T] = {
    for {
      mutaction                 <- clientMutation.prepareMutactions()
      errors                    = databaseMutactionVerifier.verify(mutaction +: mutaction.allNestedMutactions)
      _                         = if (errors.nonEmpty) throw errors.head
      databaseResults           <- databaseMutactionExecutor.executeTransactionally(mutaction)
      serverSideSubscriptions   = ServerSideSubscriptions.extractFromMutactionResults(clientMutation.project, databaseResults, requestId = "")
      publishSubscriptionEvents = SubscriptionEvents.extractFromMutactionResults(clientMutation.project, clientMutation.mutationId, databaseResults)
      _                         <- sideEffectMutactionExecutor.execute(publishSubscriptionEvents ++ serverSideSubscriptions)
      prismaNode                <- clientMutation.getReturnValue(databaseResults)
    } yield prismaNode
  }
}
