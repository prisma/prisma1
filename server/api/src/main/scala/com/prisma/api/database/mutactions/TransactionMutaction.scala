package cool.graph.api.database.mutactions

import cool.graph.api.database.DataResolver
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}


case class TransactionMutaction(clientSqlMutactions: List[ClientSqlMutaction], dataResolver: DataResolver) extends Mutaction {

  override def execute: Future[MutactionExecutionResult] = {
    val statements: Future[List[DBIOAction[Any, NoStream, Effect.All]]] = Future.sequence(clientSqlMutactions.map(_.execute)).map(_.collect { case ClientSqlStatementResult(sqlAction) => sqlAction})

    val executionResult= statements.flatMap{sqlActions =>
      val actions: immutable.Seq[DBIOAction[Any, NoStream, Effect.All]] = sqlActions
      val action: DBIOAction[Unit, NoStream, Effect.All] = DBIO.seq(actions: _*)
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

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val results: Seq[Future[Try[MutactionVerificationSuccess]]] = clientSqlMutactions.map {
      case action: ClientSqlDataChangeMutaction => action.verify(dataResolver)
      case action                               => action.verify()
    }
    val sequenced: Future[Seq[Try[MutactionVerificationSuccess]]] = Future.sequence(results)

    sequenced.map(results => results.find(_.isFailure).getOrElse(Success(MutactionVerificationSuccess())))
  }
}
