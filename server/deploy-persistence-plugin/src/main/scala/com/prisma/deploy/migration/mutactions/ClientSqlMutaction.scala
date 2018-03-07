package com.prisma.deploy.migration.mutactions

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future
import scala.util.{Success, Try}

trait ClientSqlMutaction {
  def verify(): Future[Try[Unit]] = Future.successful(Success(()))

  def execute: Future[ClientSqlStatementResult[Any]]

  def rollback: Option[Future[ClientSqlStatementResult[Any]]] = None
}

case class ClientSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All])

trait AnyMutactionExecutor extends MutactionExecutor[ClientSqlMutaction]

trait MutactionExecutor[T <: ClientSqlMutaction] {
  def execute(mutaction: T): Future[Unit]
  def rollback(mutaction: T): Future[Unit]
}

object FailingAnyMutactionExecutor extends AnyMutactionExecutor {
  override def execute(mutaction: ClientSqlMutaction) = Future.failed(new Exception(this.getClass.getSimpleName))

  override def rollback(mutaction: ClientSqlMutaction) = Future.failed(new Exception(this.getClass.getSimpleName))
}
