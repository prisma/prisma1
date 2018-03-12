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
    for {
      dbResults         <- performDatabaseMutactions(preparedMutactions.databaseMutactions, dataResolver)
      sideEffectResults <- performInParallel(preparedMutactions.sideEffectMutactions, dataResolver.project.id)
    } yield Vector(dbResults) ++ sideEffectResults
  }

  private def performDatabaseMutactions(mutactions: Vector[ClientSqlMutaction], dataResolver: DataResolver): Future[MutactionExecutionResult] = {
    import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
    import slick.jdbc.MySQLProfile.api._

    val statements: Future[Vector[DBIOAction[Any, NoStream, Effect.All]]] =
      Future.sequence(mutactions.map(_.execute)).map(_.collect { case ClientSqlStatementResult(sqlAction) => sqlAction })

    type ErrorHandler = PartialFunction[Throwable, MutactionExecutionResult]
    val combinedErrorHandler: Option[ErrorHandler] = mutactions.flatMap(_.handleErrors) match {
      case errorHandlers if errorHandlers.isEmpty => None
      case errorHandlers                          => Some(errorHandlers reduceLeft (_ orElse _))
    }

    val executionResult = statements
      .flatMap { sqlActions =>
        dataResolver.runOnClientDatabase("Transaction", DBIO.seq(sqlActions: _*).transactionally)
      }
      .map { _ =>
        MutactionExecutionSuccess()
      }

    val executionResult2 = combinedErrorHandler match {
      case Some(errorHandler) => executionResult.recover(errorHandler)
      case None               => executionResult
    }

    executionResult2
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
