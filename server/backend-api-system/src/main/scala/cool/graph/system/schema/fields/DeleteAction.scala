package cool.graph.system.schema.fields

import cool.graph.shared.models.ActionHandlerType.ActionHandlerType
import cool.graph.shared.models.ActionTriggerMutationModelMutationType._
import cool.graph.shared.models.ActionTriggerType.ActionTriggerType
import cool.graph.system.mutations._
import cool.graph.system.schema.types.{HandlerType, ModelMutationType, TriggerType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema
import sangria.schema.{OptionInputType, _}

object DeleteAction {

  val inputFields = List(InputField("actionId", IDType, description = "")).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[DeleteActionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteActionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        actionId = ad("actionId").asInstanceOf[String]
      )
    }
  }
}
