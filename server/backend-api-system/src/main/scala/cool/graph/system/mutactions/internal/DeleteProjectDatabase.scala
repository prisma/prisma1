package cool.graph.system.mutactions.internal

import cool.graph.shared.models.ProjectDatabase
import cool.graph.{SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class DeleteProjectDatabase(projectDatabase: ProjectDatabase) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    import cool.graph.system.database.tables.Tables._
    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(ProjectDatabases.filter(_.id === projectDatabase.id).delete, RelayIds.filter(_.id === projectDatabase.id).delete)))
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some(CreateOrUpdateProjectDatabase(projectDatabase).execute)

}
