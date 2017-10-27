package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{ActionHandlerWebhook, Project}
import cool.graph.system.database.tables.{ActionHandlerWebhookTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteActionHandlerWebhook(project: Project, action: cool.graph.shared.models.Action, actionHandlerWebhook: ActionHandlerWebhook)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val actionHandlerWebhooks = TableQuery[ActionHandlerWebhookTable]
    val relayIds              = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(actionHandlerWebhooks
                               .filter(_.id === actionHandlerWebhook.id)
                               .delete,
                             relayIds.filter(_.id === actionHandlerWebhook.id).delete)))
  }

  override def rollback = Some(CreateActionHandlerWebhook(project, action, actionHandlerWebhook).execute)
}
