package cool.graph.system.schema.types

import cool.graph.Types.Id
import sangria.schema._
import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import sangria.relay.Node

object ActionTriggerMutationModel {
  case class ActionTriggerMutationModelContext(project: models.Project, actionTrigger: models.ActionTriggerMutationModel) extends Node {
    override val id: Id = actionTrigger.id

  }
  lazy val Type: ObjectType[SystemUserContext, ActionTriggerMutationModelContext] =
    ObjectType(
      "ActionTriggerMutationModel",
      "This is an ActionTriggerMutationModel",
      interfaces[SystemUserContext, ActionTriggerMutationModelContext](nodeInterface),
      idField[SystemUserContext, ActionTriggerMutationModelContext] ::
        fields[SystemUserContext, ActionTriggerMutationModelContext](
        Field("fragment", StringType, resolve = _.value.actionTrigger.fragment),
        Field(
          "model",
          ModelType,
          resolve = ctx => {
            val project = ctx.value.project
            val model   = project.getModelById_!(ctx.value.actionTrigger.modelId)

            ModelContext(project, model)
          }
        ),
        Field("mutationType", ModelMutationType.Type, resolve = _.value.actionTrigger.mutationType)
      )
    )
}

object ModelMutationType {
  lazy val Type = EnumType(
    "ActionTriggerMutationModelMutationType",
    None,
    List(
      EnumValue(models.ActionTriggerMutationModelMutationType.Create.toString, value = models.ActionTriggerMutationModelMutationType.Create),
      EnumValue(models.ActionTriggerMutationModelMutationType.Update.toString, value = models.ActionTriggerMutationModelMutationType.Update),
      EnumValue(models.ActionTriggerMutationModelMutationType.Delete.toString, value = models.ActionTriggerMutationModelMutationType.Delete)
    )
  )
}
