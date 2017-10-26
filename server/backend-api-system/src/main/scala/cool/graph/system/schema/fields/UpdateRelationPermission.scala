package cool.graph.system.schema.fields

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.UserType.UserType
import cool.graph.system.mutations.UpdateRelationPermissionInput
import cool.graph.system.schema.types.{Rule, UserType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object UpdateRelationPermission {
  val inputFields: List[InputField[Any]] = List(
    InputField("id", IDType, description = ""),
    InputField("connect", OptionInputType(BooleanType), description = ""),
    InputField("disconnect", OptionInputType(BooleanType), description = ""),
    InputField("userType", OptionInputType(UserType.Type), description = ""),
    InputField("rule", OptionInputType(Rule.Type), description = ""),
    InputField("ruleName", OptionInputType(StringType), description = ""),
    InputField("ruleGraphQuery", OptionInputType(StringType), description = ""),
    InputField("ruleWebhookUrl", OptionInputType(StringType), description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("isActive", OptionInputType(BooleanType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateRelationPermissionInput] {
    val marshaller: CoercedScalaResultMarshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node): UpdateRelationPermissionInput = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateRelationPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        id = ad("id").asInstanceOf[String],
        connect = ad.get("connect").flatMap(_.asInstanceOf[Option[Boolean]]),
        disconnect = ad.get("disconnect").flatMap(_.asInstanceOf[Option[Boolean]]),
        userType = ad.get("userType").flatMap(_.asInstanceOf[Option[UserType]]),
        rule = ad.get("rule").flatMap(_.asInstanceOf[Option[CustomRule]]),
        ruleName = ad.get("ruleName").flatMap(_.asInstanceOf[Option[String]]),
        ruleGraphQuery = ad.get("ruleGraphQuery").flatMap(_.asInstanceOf[Option[String]]),
        ruleWebhookUrl = ad.get("ruleWebhookUrl").flatMap(_.asInstanceOf[Option[String]]),
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        isActive = ad.get("isActive").flatMap(_.asInstanceOf[Option[Boolean]])
      )
    }
  }
}
