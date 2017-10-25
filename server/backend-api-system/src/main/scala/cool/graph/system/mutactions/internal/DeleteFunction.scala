package cool.graph.system.mutactions.internal

import cool.graph.shared.models.{Function, Project}
import cool.graph.system.database.tables.{FunctionTable, RelayIdTable}
import cool.graph.{SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteFunction(project: Project, function: Function) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val functions = TableQuery[FunctionTable]
    val relayIds  = TableQuery[RelayIdTable]

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          functions.filter(_.id === function.id).delete,
          relayIds.filter(_.id === function.id).delete
        )
      }
    }

  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some(CreateFunction(project, function).execute)

}
