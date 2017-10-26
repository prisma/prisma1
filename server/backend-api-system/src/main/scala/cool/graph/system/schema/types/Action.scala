package cool.graph.system.schema.types

import sangria.schema._
import cool.graph.shared.models
import cool.graph.system.schema.types.ActionTriggerMutationModel.ActionTriggerMutationModelContext
import sangria.relay.Node

object _Action {
  case class ActionContext(project: models.Project, action: models.Action) extends Node {
    override val id = action.id

  }
  lazy val Type: ObjectType[Unit, ActionContext] = ObjectType(
    "Action",
    "This is an action",
    interfaces[Unit, ActionContext](nodeInterface),
    idField[Unit, ActionContext] ::
      fields[Unit, ActionContext](
      Field("isActive", BooleanType, resolve = _.value.action.isActive),
      Field("description", OptionType(StringType), resolve = _.value.action.description),
      Field("triggerType", TriggerType.Type, resolve = _.value.action.triggerType),
      Field("handlerType", HandlerType.Type, resolve = _.value.action.handlerType),
      Field(
        "triggerMutationModel",
        OptionType(ActionTriggerMutationModelType),
        resolve = ctx => ctx.value.action.triggerMutationModel.map(ActionTriggerMutationModelContext(ctx.value.project, _))
      ),
      Field("triggerMutationRelation", OptionType(ActionTriggerMutationRelationType), resolve = _.value.action.triggerMutationRelation),
      Field("handlerWebhook", OptionType(ActionHandlerWebhookType), resolve = _.value.action.handlerWebhook)
    )
  )
}

object TriggerType {
  lazy val Type = {
    EnumType(
      "ActionTriggerType",
      None,
      List(
        EnumValue(models.ActionTriggerType.MutationModel.toString, value = models.ActionTriggerType.MutationModel),
        EnumValue(models.ActionTriggerType.MutationRelation.toString, value = models.ActionTriggerType.MutationRelation)
      )
    )
  }
}

object HandlerType {
  lazy val Type = {
    EnumType("ActionHandlerType",
             None,
             List(
               EnumValue(models.ActionHandlerType.Webhook.toString, value = models.ActionHandlerType.Webhook)
             ))
  }
}
