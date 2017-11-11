package cool.graph

import cool.graph.client.database.DataResolver
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import scala.util.{Success, Try}

abstract class Mutaction {
  def verify(): Future[Try[MutactionVerificationSuccess]] = Future.successful(Success(MutactionVerificationSuccess()))
  def execute: Future[MutactionExecutionResult]
  def handleErrors: Option[PartialFunction[Throwable, MutactionExecutionResult]] = None
  def rollback: Option[Future[MutactionExecutionResult]]                         = None
  def postExecute: Future[Boolean]                                               = Future.successful(true)
}

abstract class ClientSqlMutaction extends Mutaction {
  override def execute: Future[ClientSqlStatementResult[Any]]
  override def rollback: Option[Future[ClientSqlStatementResult[Any]]] = None
}

trait ClientSqlSchemaChangeMutaction extends ClientSqlMutaction
trait ClientSqlDataChangeMutaction extends ClientSqlMutaction {
  def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = Future.successful(Success(MutactionVerificationSuccess()))
}

abstract class SystemSqlMutaction extends Mutaction {
  override def execute: Future[SystemSqlStatementResult[Any]]
  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = None
}

case class MutactionVerificationSuccess()

trait MutactionExecutionResult
case class MutactionExecutionSuccess()                                                        extends MutactionExecutionResult
case class ClientSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All]) extends MutactionExecutionResult
case class SystemSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All]) extends MutactionExecutionResult

case class ClientMutactionNoop() extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]]          = Future.successful(ClientSqlStatementResult(sqlAction = DBIO.successful(None)))
  override def rollback: Option[Future[ClientSqlStatementResult[Any]]] = Some(Future.successful(ClientSqlStatementResult(sqlAction = DBIO.successful(None))))
}
