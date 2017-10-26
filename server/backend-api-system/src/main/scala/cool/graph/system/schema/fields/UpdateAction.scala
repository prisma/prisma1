package cool.graph.system.schema.fields

import cool.graph.shared.models.ActionHandlerType.ActionHandlerType
import cool.graph.shared.models.ActionTriggerMutationModelMutationType._
import cool.graph.shared.models.ActionTriggerType.ActionTriggerType
import cool.graph.system.mutations._
import cool.graph.system.schema.types.{HandlerType, ModelMutationType, TriggerType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema
import sangria.schema.{OptionInputType, _}

object UpdateAction {

  val inputFields = List(
    InputField("actionId", IDType, description = ""),
    InputField("isActive", OptionInputType(BooleanType), description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("triggerType", OptionInputType(TriggerType.Type), description = ""),
    InputField("handlerType", OptionInputType(HandlerType.Type), description = ""),
    InputField("handlerWebhook", OptionInputType(AddAction.handlerWebhook), description = ""),
    InputField("triggerMutationModel", OptionInputType(AddAction.triggerMutationModel), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateActionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateActionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        actionId = ad("actionId").asInstanceOf[String],
        isActive = ad.get("isActive").flatMap(_.asInstanceOf[Option[Boolean]]),
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        triggerType = ad
          .get("triggerType")
          .flatMap(_.asInstanceOf[Option[ActionTriggerType]]),
        handlerType = ad
          .get("handlerType")
          .flatMap(_.asInstanceOf[Option[ActionHandlerType]]),
        webhookUrl = ad
          .get("handlerWebhook")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]])
          .map(_("url").asInstanceOf[String]),
        webhookIsAsync = ad
          .get("handlerWebhook")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]])
          .flatMap(_.get("isAsync").flatMap(_.asInstanceOf[Option[Boolean]])),
        actionTriggerMutationModel = ad
          .get("triggerMutationModel")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]])
          .map(x =>
            AddActionTriggerModelInput(
              modelId = x("modelId").asInstanceOf[String],
              mutationType = x("mutationType")
                .asInstanceOf[ActionTriggerMutationModelMutationType],
              fragment = x("fragment").asInstanceOf[String]
          ))
      )
    }
  }
}
