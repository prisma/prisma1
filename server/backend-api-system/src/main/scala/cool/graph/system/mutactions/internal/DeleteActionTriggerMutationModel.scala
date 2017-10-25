package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{ActionTriggerMutationModelTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteActionTriggerMutationModel(project: Project, actionTriggerMutationModel: ActionTriggerMutationModel) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val actionTriggerMutationModels =
      TableQuery[ActionTriggerMutationModelTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(actionTriggerMutationModels.filter(_.id === actionTriggerMutationModel.id).delete,
                             relayIds.filter(_.id === actionTriggerMutationModel.id).delete)))
  }
}
