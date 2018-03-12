package com.prisma.api.database.mutactions

import com.prisma.api.database.DataResolver
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

case class TransactionMutaction(clientSqlMutactions: List[ClientSqlMutaction], dataResolver: DataResolver) extends Mutaction {

  override def execute: Future[MutactionExecutionResult] = {
    val statements: Future[List[DBIOAction[Any, NoStream, Effect.All]]] =
      Future.sequence(clientSqlMutactions.map(_.execute)).map(_.collect { case ClientSqlStatementResult(sqlAction) => sqlAction })

    val executionResult = statements.flatMap { sqlActions =>
      val actions: immutable.Seq[DBIOAction[Any, NoStream, Effect.All]] = sqlActions
      val action: DBIOAction[Unit, NoStream, Effect.All]                = DBIO.seq(actions: _*)
      dataResolver.runOnClientDatabase("Transaction", action.transactionally)
    }

    executionResult.map(_ => MutactionExecutionSuccess())
  }

  override def handleErrors: Option[PartialFunction[Throwable, MutactionExecutionResult]] = {
    clientSqlMutactions.flatMap(_.handleErrors) match {
      case errorHandlers if errorHandlers.isEmpty => None
      case errorHandlers                          => Some(errorHandlers reduceLeft (_ orElse _))
    }
  }

  override def verify(): Try[MutactionVerificationSuccess] = {
    val results = clientSqlMutactions.map(_.verify())
    results.find(_.isFailure).getOrElse(Success(MutactionVerificationSuccess()))
  }
}
