package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Integration, Project}
import cool.graph.system.database.tables.{IntegrationTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteIntegration(project: Project, integration: Integration) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val integrations = TableQuery[IntegrationTable]
    val relayIds     = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(sqlAction = DBIO.seq(integrations.filter(_.id === integration.id).delete, relayIds.filter(_.id === integration.id).delete)))
  }

  override def rollback = Some(CreateIntegration(project, integration).execute)

}
