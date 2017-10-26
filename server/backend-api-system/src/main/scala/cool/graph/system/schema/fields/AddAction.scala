package cool.graph.system.schema.fields

import cool.graph.shared.models.ActionHandlerType.ActionHandlerType
import cool.graph.shared.models.ActionTriggerMutationModelMutationType._
import cool.graph.shared.models.ActionTriggerType.ActionTriggerType
import cool.graph.system.mutations._
import cool.graph.system.schema.types.{HandlerType, ModelMutationType, TriggerType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema
import sangria.schema.{OptionInputType, _}

object AddAction {

  val handlerWebhook = InputObjectType(
    name = "ActionHandlerWebhookEmbed",
    fields = List(
      InputField("url", StringType),
      InputField("isAsync", OptionInputType(BooleanType))
    )
  )

  val triggerMutationModel = InputObjectType(
    name = "ActionTriggerModelMutationEmbed",
    fields = List(
      InputField("fragment", StringType),
      InputField("modelId", IDType),
      InputField("mutationType", ModelMutationType.Type)
    )
  )

  val inputFields = List(
    InputField("projectId", IDType, description = ""),
    InputField("isActive", BooleanType, description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("triggerType", TriggerType.Type, description = ""),
    InputField("handlerType", HandlerType.Type, description = ""),
    InputField("handlerWebhook", OptionInputType(handlerWebhook), description = ""),
    InputField("triggerMutationModel", OptionInputType(triggerMutationModel), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddActionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddActionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        isActive = ad("isActive").asInstanceOf[Boolean],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        triggerType = ad("triggerType").asInstanceOf[ActionTriggerType],
        handlerType = ad("handlerType").asInstanceOf[ActionHandlerType],
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
