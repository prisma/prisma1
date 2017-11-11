package cool.graph.system.mutactions.internal

import cool.graph.shared.models._
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.{FunctionTable, RelayIdTable}
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.Try

case class CreateFunction(project: Project, function: Function) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val functions = TableQuery[FunctionTable]
    val relayIds  = TableQuery[RelayIdTable]
    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          functions += ModelToDbMapper.convertFunction(project, function),
          relayIds += cool.graph.system.database.tables.RelayId(function.id, "Function")
        )
      }
    }
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] = Some(DeleteFunction(project, function).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = FunctionVerification.verifyFunction(function, project)
}
