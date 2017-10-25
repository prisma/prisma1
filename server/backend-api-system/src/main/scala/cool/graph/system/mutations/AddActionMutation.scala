package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.ActionInputIsInconsistent
import cool.graph.shared.models
import cool.graph.shared.models.ActionHandlerType.ActionHandlerType
import cool.graph.shared.models.ActionTriggerMutationModelMutationType.ActionTriggerMutationModelMutationType
import cool.graph.shared.models.ActionTriggerType.ActionTriggerType
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateAction, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddActionMutation(
    client: models.Client,
    project: models.Project,
    args: AddActionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[AddActionMutationPayload] {

  var newAction: Option[models.Action] = None

  def verifyArgs: Option[InvalidInput] = {
    if (args.triggerType == ActionTriggerType.MutationModel && args.actionTriggerMutationModel.isEmpty) {
      return Some(InvalidInput(ActionInputIsInconsistent(s"Specified triggerType '${ActionTriggerType.MutationModel}' requires 'triggerMutationModel'")))
    }

    if (args.handlerType == models.ActionHandlerType.Webhook && args.webhookUrl.isEmpty) {
      return Some(InvalidInput(ActionInputIsInconsistent(s"Specified triggerType '${models.ActionHandlerType.Webhook}' requires 'handlerWebhook'")))
    }

    None
  }

  override def prepareActions(): List[Mutaction] = {

    val argsValidationError = verifyArgs
    if (argsValidationError.isDefined) {
      actions = List(argsValidationError.get)
      return actions
    }

    newAction = Some(
      models.Action(
        id = Cuid.createCuid(),
        isActive = args.isActive,
        description = args.description,
        triggerType = args.triggerType,
        handlerType = args.handlerType,
        triggerMutationModel = args.actionTriggerMutationModel.map(t =>
          ActionTriggerMutationModel(id = Cuid.createCuid(), modelId = t.modelId, mutationType = t.mutationType, fragment = t.fragment)),
        handlerWebhook = args.webhookUrl.map(url => ActionHandlerWebhook(id = Cuid.createCuid(), url = url, isAsync = args.webhookIsAsync.getOrElse(true)))
      ))

    actions ++= CreateAction.generateAddActionMutactions(newAction.get, project = project)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue(): Option[AddActionMutationPayload] = {
    Some(
      AddActionMutationPayload(clientMutationId = args.clientMutationId,
                               project = project.copy(actions = project.actions :+ newAction.get),
                               action = newAction.get))
  }
}

case class AddActionMutationPayload(clientMutationId: Option[String], project: models.Project, action: models.Action) extends Mutation

case class AddActionTriggerModelInput(modelId: String, mutationType: ActionTriggerMutationModelMutationType, fragment: String)

case class AddActionInput(clientMutationId: Option[String],
                          projectId: String,
                          isActive: Boolean,
                          description: Option[String],
                          triggerType: ActionTriggerType,
                          handlerType: ActionHandlerType,
                          webhookUrl: Option[String],
                          webhookIsAsync: Option[Boolean],
                          actionTriggerMutationModel: Option[AddActionTriggerModelInput])
