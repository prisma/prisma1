package cool.graph.client.mutations

import cool.graph.Types.Id
import cool.graph.client.ClientInjector
import cool.graph.client.mutactions._
import cool.graph.shared.models.{ActionTriggerMutationModelMutationType, Project}
import cool.graph.{DataItem, Mutaction}

import scala.collection.immutable.Seq

object ActionWebhooks {
  def extractFromCreateMutactions(project: Project, mutactions: Seq[CreateDataItem], mutationId: Id, requestId: String)(
      implicit injector: ClientInjector): Seq[Mutaction] = {
    for {
      newItem <- mutactions
      action  <- project.actionsFor(newItem.model.id, ActionTriggerMutationModelMutationType.Create)
    } yield {
      if (action.handlerWebhook.get.isAsync) {
        ActionWebhookForCreateDataItemAsync(
          model = newItem.model,
          project = project,
          nodeId = newItem.id,
          action = action,
          mutationId = mutationId,
          requestId = requestId
        )
      } else {
        ActionWebhookForCreateDataItemSync(
          model = newItem.model,
          project = project,
          nodeId = newItem.id,
          action = action,
          mutationId = mutationId,
          requestId = requestId
        )
      }
    }
  }

  def extractFromUpdateMutactions(project: Project, mutactions: Seq[UpdateDataItem], mutationId: Id, requestId: String, previousValues: DataItem)(
      implicit injector: ClientInjector): Seq[Mutaction] = {
    for {
      updatedItem <- mutactions
      action      <- project.actionsFor(updatedItem.model.id, ActionTriggerMutationModelMutationType.Update)
    } yield {
      if (action.handlerWebhook.get.isAsync) {
        ActionWebhookForUpdateDataItemAsync(
          model = updatedItem.model,
          project = project,
          nodeId = updatedItem.id,
          action = action,
          updatedFields = updatedItem.namesOfUpdatedFields,
          mutationId = mutationId,
          requestId = requestId,
          previousValues = previousValues
        )
      } else {
        ActionWebhookForUpdateDataItemSync(
          model = updatedItem.model,
          project = project,
          nodeId = updatedItem.id,
          action = action,
          updatedFields = updatedItem.namesOfUpdatedFields,
          mutationId = mutationId,
          requestId = requestId,
          previousValues = previousValues
        )
      }
    }
  }
}
