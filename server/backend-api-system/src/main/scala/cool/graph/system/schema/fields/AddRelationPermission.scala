package cool.graph.system.schema.fields

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.UserType.UserType
import cool.graph.system.mutations.AddRelationPermissionInput
import cool.graph.system.schema.types.{Rule, UserType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object AddRelationPermission {
  val inputFields = List(
    InputField("relationId", IDType, description = ""),
    InputField("connect", BooleanType, description = ""),
    InputField("disconnect", BooleanType, description = ""),
    InputField("userType", UserType.Type, description = ""),
    InputField("rule", Rule.Type, description = ""),
    InputField("ruleName", OptionInputType(StringType), description = ""),
    InputField("ruleGraphQuery", OptionInputType(StringType), description = ""),
    InputField("ruleWebhookUrl", OptionInputType(StringType), description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("isActive", BooleanType, description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddRelationPermissionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddRelationPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        relationId = ad("relationId").asInstanceOf[String],
        connect = ad("connect").asInstanceOf[Boolean],
        disconnect = ad("disconnect").asInstanceOf[Boolean],
        userType = ad("userType").asInstanceOf[UserType],
        rule = ad("rule").asInstanceOf[CustomRule],
        ruleName = ad.get("ruleName").flatMap(_.asInstanceOf[Option[String]]),
        ruleGraphQuery = ad.get("ruleGraphQuery").flatMap(_.asInstanceOf[Option[String]]),
        ruleWebhookUrl = ad.get("ruleWebhookUrl").flatMap(_.asInstanceOf[Option[String]]),
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        isActive = ad("isActive").asInstanceOf[Boolean]
      )
    }
  }
}
