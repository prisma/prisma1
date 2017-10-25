package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Action, Project}
import cool.graph.system.database.tables.{ActionTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteAction(project: Project, action: Action) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val actions  = TableQuery[ActionTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(actions.filter(_.id === action.id).delete, relayIds.filter(_.id === action.id).delete)))
  }

  override def rollback = Some(new CreateAction(project, action).execute)
}
