package cool.graph

import cool.graph.client.database.DataResolver
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

case class Transaction(clientSqlMutactions: List[ClientSqlMutaction], dataResolver: DataResolver) extends Mutaction {

  override def execute: Future[MutactionExecutionResult] = {
    Future
      .sequence(clientSqlMutactions.map(_.execute))
      .map(_.collect {
        case ClientSqlStatementResult(sqlAction) => sqlAction
      })
      .flatMap(
        sqlActions =>
          dataResolver
            .runOnClientDatabase("Transaction", DBIO.seq(sqlActions: _*)) //.transactionally # Due to https://github.com/slick/slick/pull/1461 not being in a stable release yet
      )
      .map(_ => MutactionExecutionSuccess())
  }

  override def handleErrors: Option[PartialFunction[Throwable, MutactionExecutionResult]] = {
    clientSqlMutactions.flatMap(_.handleErrors) match {
      case errorHandlers if errorHandlers.isEmpty => None
      case errorHandlers                          => Some(errorHandlers reduceLeft (_ orElse _))
    }
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val results: Seq[Future[Try[MutactionVerificationSuccess]]] = clientSqlMutactions.map {
      case action: ClientSqlDataChangeMutaction => action.verify(dataResolver)
      case action                               => action.verify()
    }
    val sequenced: Future[Seq[Try[MutactionVerificationSuccess]]] = Future.sequence(results)

    sequenced.map(results => results.find(_.isFailure).getOrElse(Success(MutactionVerificationSuccess())))
  }
}
