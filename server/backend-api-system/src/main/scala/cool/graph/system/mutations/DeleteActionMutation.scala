package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteActionMutation(
    client: models.Client,
    project: models.Project,
    args: DeleteActionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteActionMutationPayload] {

  var deletedAction: models.Action = project.getActionById_!(args.actionId)

  override def prepareActions(): List[Mutaction] = {

    // note: handlers and triggers does not cascade delete because we think it
    // might make sense to model them as individual entities in the ui

    if (deletedAction.handlerWebhook.isDefined)
      actions :+= DeleteActionHandlerWebhook(project, deletedAction, deletedAction.handlerWebhook.get)

    if (deletedAction.triggerMutationModel.isDefined)
      actions :+= DeleteActionTriggerMutationModel(project, deletedAction.triggerMutationModel.get)

    actions :+= DeleteAction(project, deletedAction)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[DeleteActionMutationPayload] = {
    Some(
      DeleteActionMutationPayload(clientMutationId = args.clientMutationId,
                                  project = project.copy(actions = project.actions.filter(_.id != deletedAction.id)),
                                  action = deletedAction))
  }
}

case class DeleteActionMutationPayload(clientMutationId: Option[String], project: models.Project, action: models.Action) extends Mutation

case class DeleteActionInput(clientMutationId: Option[String], actionId: String)
