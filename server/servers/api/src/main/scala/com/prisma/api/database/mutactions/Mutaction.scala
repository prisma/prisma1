package com.prisma.api.database.mutactions

import com.prisma.api.database.DataResolver
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future
import scala.util.{Success, Try}

abstract class Mutaction {
  def verify(): Try[MutactionVerificationSuccess] = Success(MutactionVerificationSuccess())
  def execute: Future[MutactionExecutionResult]
  def handleErrors: Option[PartialFunction[Throwable, MutactionExecutionResult]] = None
}

abstract class ClientSqlMutaction extends Mutaction {
  override def execute: Future[ClientSqlStatementResult[Any]]
}

trait ClientSqlDataChangeMutaction extends ClientSqlMutaction

case class MutactionVerificationSuccess()

trait MutactionExecutionResult
case class MutactionExecutionSuccess()                                                        extends MutactionExecutionResult
case class ClientSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All]) extends MutactionExecutionResult
