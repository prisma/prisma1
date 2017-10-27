package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.models.{Action, ActionHandlerWebhook, ActionTriggerMutationModel, Project}
import cool.graph.system.database.tables.{ActionTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateAction(project: Project, action: Action) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful({
      val actions  = TableQuery[ActionTable]
      val relayIds = TableQuery[RelayIdTable]
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          actions +=
            cool.graph.system.database.tables.Action(action.id, project.id, action.isActive, action.triggerType, action.handlerType, action.description),
          relayIds +=
            cool.graph.system.database.tables.RelayId(action.id, "Action")
        ))
    })
  }

  override def rollback = Some(DeleteAction(project, action).execute)
}

object CreateAction {
  def generateAddActionMutactions(action: Action, project: Project): List[Mutaction] = {
    def createAction = CreateAction(project = project, action = action)

    def createHandlerWebhook: Option[CreateActionHandlerWebhook] =
      action.handlerWebhook.map(
        h =>
          CreateActionHandlerWebhook(
            project = project,
            action = action,
            actionHandlerWebhook = ActionHandlerWebhook(id = Cuid.createCuid(), url = h.url, h.isAsync)
        ))

    def createActionTriggerMutationModel: Option[CreateActionTriggerMutationModel] =
      action.triggerMutationModel.map(
        t =>
          CreateActionTriggerMutationModel(
            project = project,
            action = action,
            actionTriggerMutationModel = ActionTriggerMutationModel(
              id = Cuid.createCuid(),
              modelId = t.modelId,
              mutationType = t.mutationType,
              fragment = t.fragment
            )
        ))

    List(Some(createAction), createHandlerWebhook, createActionTriggerMutationModel).flatten
  }
}
