package cool.graph.deploy.migration.mutactions

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future
import scala.util.{Success, Try}

trait ClientSqlMutaction {
  def verify(): Future[Try[Unit]] = Future.successful(Success(()))

  def execute: Future[ClientSqlStatementResult[Any]]

  def rollback: Option[Future[ClientSqlStatementResult[Any]]] = None
}

case class ClientSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All])
