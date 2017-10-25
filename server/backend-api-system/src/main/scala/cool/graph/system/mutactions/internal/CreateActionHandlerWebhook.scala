package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Action, ActionHandlerWebhook, Project}
import cool.graph.system.database.tables.{ActionHandlerWebhookTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Success, Try}

case class CreateActionHandlerWebhook(project: Project, action: Action, actionHandlerWebhook: ActionHandlerWebhook) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val actionHandlerWebhooks = TableQuery[ActionHandlerWebhookTable]
    val relayIds              = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          actionHandlerWebhooks += cool.graph.system.database.tables
            .ActionHandlerWebhook(actionHandlerWebhook.id, action.id, actionHandlerWebhook.url, actionHandlerWebhook.isAsync),
          relayIds += cool.graph.system.database.tables
            .RelayId(actionHandlerWebhook.id, "ActionHandlerWebhook")
        )))
  }

  override def rollback = Some(DeleteActionHandlerWebhook(project, action, actionHandlerWebhook).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    // todo: verify is valid url
    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
