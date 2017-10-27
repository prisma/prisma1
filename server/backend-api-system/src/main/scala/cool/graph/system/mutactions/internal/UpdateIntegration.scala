package cool.graph.system.mutactions.internal

import cool.graph.shared.models.{Integration, Project}
import cool.graph.system.database.tables.{IntegrationTable, RelayIdTable}
import cool.graph.{SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateIntegration(project: Project, oldIntegration: Integration, integration: Integration) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful({
      val integrations = TableQuery[IntegrationTable]
      val relayIds     = TableQuery[RelayIdTable]
      println("Updating isEnabled of integration " + integration.isEnabled.toString)
      SystemSqlStatementResult(
        sqlAction = DBIO.seq({
          val q = for { i <- integrations if i.id === integration.id } yield i.isEnabled
          q.update(integration.isEnabled)
        })
      )
    })
  }

  override def rollback = Some(UpdateIntegration(project, oldIntegration = oldIntegration, integration = oldIntegration).execute)

}
