package cool.graph.system.mutactions.internal

import cool.graph.shared.errors.UserInputErrors.ActionInputIsInconsistent
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{ActionTriggerMutationModelTable, RelayIdTable}
import cool.graph.shared.models.{Action, ActionTriggerMutationModel, Project}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateActionTriggerMutationModel(project: Project, action: Action, actionTriggerMutationModel: ActionTriggerMutationModel)
    extends SystemSqlMutaction {
  override def execute = {
    val actionTriggerMutationModels =
      TableQuery[ActionTriggerMutationModelTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          actionTriggerMutationModels +=
            cool.graph.system.database.tables.ActionTriggerMutationModel(
              actionTriggerMutationModel.id,
              action.id,
              actionTriggerMutationModel.modelId,
              actionTriggerMutationModel.mutationType,
              actionTriggerMutationModel.fragment
            ),
          relayIds += cool.graph.system.database.tables
            .RelayId(actionTriggerMutationModel.id, "ActionTriggerMutationModel")
        )))
  }

  override def handleErrors =
    Some({
      case e => throw e
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
//      case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
//          if e.getErrorCode == 1452 =>
//        ActionInputIsInconsistent("Specified model does not exist")
    })

  override def rollback = Some(DeleteActionTriggerMutationModel(project, actionTriggerMutationModel).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    // todo: verify is valid url

    project.getModelById(actionTriggerMutationModel.modelId) match {
      case Some(_) => Future.successful(Success(MutactionVerificationSuccess()))
      case None    => Future.successful(Failure(ActionInputIsInconsistent("Specified model does not exist")))
    }
  }
}
