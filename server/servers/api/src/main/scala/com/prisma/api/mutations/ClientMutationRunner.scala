package com.prisma.api.mutations

import com.prisma.api.ApiMetrics
import com.prisma.api.connector.DatabaseMutactionExecutor
import com.prisma.api.database.mutactions._
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, SideEffectMutactionExecutor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor,
      databaseMutactionVerifier: DatabaseMutactionVerifier
  ): Future[T] = {
    for {
      preparedMutactions <- clientMutation.prepareMutactions()
      errors             = databaseMutactionVerifier.verify(preparedMutactions.databaseMutactions)
      _                  = if (errors.nonEmpty) throw errors.head
      _                  <- performMutactions(preparedMutactions, clientMutation.projectId, databaseMutactionExecutor, sideEffectMutactionExecutor)
      dataItem           <- clientMutation.getReturnValue
    } yield dataItem
  }

  private def performMutactions(
      preparedMutactions: PreparedMutactions,
      projectId: String,
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor
  ): Future[Unit] = {
    for {
      _ <- databaseMutactionExecutor.execute(preparedMutactions.databaseMutactions)
      _ <- sideEffectMutactionExecutor.execute(preparedMutactions.sideEffectMutactions)
    } yield ()
  }

  private def runWithTiming(mutaction: Mutaction, projectId: String): Future[MutactionExecutionResult] = {
    performWithTiming(
      s"execute ${mutaction.getClass.getSimpleName}", {
        mutaction match {
          case mut: ClientSqlDataChangeMutaction =>
            ApiMetrics.mutactionTimer.timeFuture(projectId) {
              runWithErrorHandler(mut)
            }
          case mut =>
            runWithErrorHandler(mut)
        }
      }
    )
  }

  private def runWithErrorHandler(mutaction: Mutaction): Future[MutactionExecutionResult] = {
    mutaction.handleErrors match {
      case Some(errorHandler) =>
//        mutaction.execute.recover(errorHandler)
        mutaction.execute.recoverWith {
          case error =>
            errorHandler.lift(error) match {
              case Some(newError) => throw newError
              case None           => throw error
            }
        }
      case None =>
        mutaction.execute
    }
  }

  private def performWithTiming[A](name: String, f: Future[A]): Future[A] = {
    //    val begin = System.currentTimeMillis()
    //    f andThen {
    //      case x =>
    //        mutactionTimings :+= Timing(name, System.currentTimeMillis() - begin)
    //        x
    //    }

    f
  }
}
