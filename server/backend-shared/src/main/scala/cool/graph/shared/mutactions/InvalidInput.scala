package cool.graph.shared.mutactions

import cool.graph._
import cool.graph.shared.errors.GeneralError
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class InvalidInput(error: GeneralError, isInvalid: Future[Boolean] = Future.successful(true))(implicit inj: Injector) extends Mutaction with Injectable {

  override def execute: Future[MutactionExecutionResult] = Future.successful(MutactionExecutionSuccess())

  override def verify(): Future[Try[MutactionVerificationSuccess]] = isInvalid.map {
    case true  => Failure(error)
    case false => Success(MutactionVerificationSuccess())
  }
}

case class InvalidInputClientSqlMutaction(error: GeneralError, isInvalid: () => Future[Boolean] = () => Future.successful(true)) extends ClientSqlMutaction {
  lazy val isInvalidResult = isInvalid()

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(ClientSqlStatementResult(sqlAction = DBIO.seq()))

  override def verify(): Future[Try[MutactionVerificationSuccess]] =
    isInvalidResult.map {
      case true  => Failure(error)
      case false => Success(MutactionVerificationSuccess())
    }
}
