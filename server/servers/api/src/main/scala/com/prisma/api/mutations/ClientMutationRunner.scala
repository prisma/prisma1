package com.prisma.api.mutations

import com.prisma.api.ApiMetrics
import com.prisma.api.database.DataResolver
import com.prisma.api.database.mutactions._
import com.prisma.api.schema.GeneralError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      dataResolver: DataResolver
  ): Future[T] = {
    for {
      preparedMutactions <- clientMutation.prepareMutactions()
      errors             = verifyMutactions(preparedMutactions)
      _                  = if (errors.nonEmpty) throw errors.head
      executionResults   <- performMutactions(preparedMutactions, dataResolver)
      dataItem <- {
        executionResults
          .filter(_.isInstanceOf[GeneralError])
          .map(_.asInstanceOf[GeneralError]) match {
          case errors if errors.nonEmpty => throw errors.head
          case _                         => clientMutation.getReturnValue
        }
      }
    } yield dataItem
  }

  private def verifyMutactions(preparedMutactions: PreparedMutactions): Vector[GeneralError] = {
    val verifications = preparedMutactions.allMutactions.map(_.verify())
    val errors        = verifications.collect { case Failure(x: GeneralError) => x }
    errors
  }

  private def performMutactions(preparedMutactions: PreparedMutactions, dataResolver: DataResolver): Future[Vector[MutactionExecutionResult]] = {
    val projectId   = dataResolver.project.id
    val transaction = TransactionMutaction(preparedMutactions.databaseMutactions.toList, dataResolver)
    for {
      dbResults         <- runWithTiming(transaction, projectId)
      sideEffectResults <- performInParallel(preparedMutactions.sideEffectMutactions, projectId)
    } yield Vector(dbResults) ++ sideEffectResults
  }

  private def performInParallel(mutactions: Vector[Mutaction], projectId: String): Future[Vector[MutactionExecutionResult]] = {
    Future.sequence(mutactions.map(m => runWithTiming(m, projectId)))
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
      case Some(errorHandler) => mutaction.execute.recover(errorHandler)
      case None               => mutaction.execute
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
