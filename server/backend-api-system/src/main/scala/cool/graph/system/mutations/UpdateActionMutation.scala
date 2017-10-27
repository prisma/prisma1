package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.ActionHandlerType.ActionHandlerType
import cool.graph.shared.models.ActionTriggerMutationModelMutationType.ActionTriggerMutationModelMutationType
import cool.graph.shared.models.ActionTriggerType.ActionTriggerType
import cool.graph.shared.models.{Action, ActionHandlerWebhook, ActionTriggerMutationModel}
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateActionMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateActionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateActionMutationPayload] {

  val existingAction: Action = project.getActionById_!(args.actionId)

  var updatedAction: models.Action = mergeInputValuesToField(existingAction, args)

  def mergeInputValuesToField(existingAction: Action, updateValues: UpdateActionInput): Action = {
    existingAction.copy(
      isActive = updateValues.isActive.getOrElse(existingAction.isActive),
      triggerType = updateValues.triggerType.getOrElse(existingAction.triggerType),
      handlerType = updateValues.handlerType.getOrElse(existingAction.handlerType),
      description = updateValues.description match {
        case Some(x) => Some(x)
        case None    => existingAction.description
      }
    )
  }

  override def prepareActions(): List[Mutaction] = {

    actions :+= UpdateAction(project = project, oldAction = existingAction, action = updatedAction)

    if (args.webhookUrl.isDefined) {
      if (existingAction.handlerWebhook.isDefined) {
        actions :+= DeleteActionHandlerWebhook(project, existingAction, existingAction.handlerWebhook.get)
      }

      val actionHandlerWebhook =
        ActionHandlerWebhook(id = Cuid.createCuid(), url = args.webhookUrl.get, args.webhookIsAsync.getOrElse(true))

      updatedAction = updatedAction.copy(handlerWebhook = Some(actionHandlerWebhook))

      actions :+= CreateActionHandlerWebhook(
        project = project,
        action = updatedAction,
        actionHandlerWebhook = actionHandlerWebhook
      )
    }

    if (args.actionTriggerMutationModel.isDefined) {
      if (existingAction.triggerMutationModel.isDefined) {
        actions :+= DeleteActionTriggerMutationModel(project, existingAction.triggerMutationModel.get)
      }

      val actionTriggerMutationModel = ActionTriggerMutationModel(
        id = Cuid.createCuid(),
        modelId = args.actionTriggerMutationModel.get.modelId,
        mutationType = args.actionTriggerMutationModel.get.mutationType,
        fragment = args.actionTriggerMutationModel.get.fragment
      )

      updatedAction = updatedAction.copy(triggerMutationModel = Some(actionTriggerMutationModel))

      actions :+= CreateActionTriggerMutationModel(
        project = project,
        action = updatedAction,
        actionTriggerMutationModel = actionTriggerMutationModel
      )
    }

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[UpdateActionMutationPayload] = {
    Some(
      UpdateActionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project.copy(actions = project.actions.filter(_.id != updatedAction.id) :+ updatedAction),
        action = updatedAction
      ))
  }
}

case class UpdateActionMutationPayload(clientMutationId: Option[String], project: models.Project, action: models.Action) extends Mutation

case class UpdateActionTriggerModelInput(modelId: String, mutationType: ActionTriggerMutationModelMutationType, fragment: String)

case class UpdateActionInput(clientMutationId: Option[String],
                             actionId: String,
                             isActive: Option[Boolean],
                             description: Option[String],
                             triggerType: Option[ActionTriggerType],
                             handlerType: Option[ActionHandlerType],
                             webhookUrl: Option[String],
                             webhookIsAsync: Option[Boolean],
                             actionTriggerMutationModel: Option[AddActionTriggerModelInput])
