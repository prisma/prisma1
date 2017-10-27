package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{IntegrationTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateIntegration(project: Project, integration: Integration) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful({
      val integrations = TableQuery[IntegrationTable]
      val relayIds     = TableQuery[RelayIdTable]
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          integrations +=
            cool.graph.system.database.tables.Integration(integration.id, integration.isEnabled, integration.integrationType, integration.name, project.id),
          relayIds +=
            cool.graph.system.database.tables
              .RelayId(integration.id, "Integration")
        ))
    })
  }

  override def rollback = Some(DeleteIntegration(project, integration).execute)

}
