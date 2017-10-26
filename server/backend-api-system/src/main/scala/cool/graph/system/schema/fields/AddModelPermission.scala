package cool.graph.system.schema.fields

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.ModelOperation.ModelOperation
import cool.graph.shared.models.UserType.UserType
import cool.graph.system.mutations.AddModelPermissionInput
import cool.graph.system.schema.types.{Operation, Rule, UserType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{ListInputType, OptionInputType, _}

object AddModelPermission {
  val inputFields = List(
    InputField("modelId", IDType, description = ""),
    InputField("operation", Operation.Type, description = ""),
    InputField("userType", UserType.Type, description = ""),
    InputField("rule", Rule.Type, description = ""),
    InputField("ruleName", OptionInputType(StringType), description = ""),
    InputField("ruleGraphQuery", OptionInputType(StringType), description = ""),
    InputField("ruleWebhookUrl", OptionInputType(StringType), description = ""),
    InputField("fieldIds", ListInputType(StringType), description = ""),
    InputField("applyToWholeModel", BooleanType, description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("isActive", BooleanType, description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddModelPermissionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddModelPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelId = ad("modelId").asInstanceOf[String],
        operation = ad("operation").asInstanceOf[ModelOperation],
        userType = ad("userType").asInstanceOf[UserType],
        rule = ad("rule").asInstanceOf[CustomRule],
        ruleName = ad.get("ruleName").flatMap(_.asInstanceOf[Option[String]]),
        ruleGraphQuery = ad.get("ruleGraphQuery").flatMap(_.asInstanceOf[Option[String]]),
        ruleWebhookUrl = ad.get("ruleWebhookUrl").flatMap(_.asInstanceOf[Option[String]]),
        fieldIds = ad("fieldIds").asInstanceOf[Vector[String]].toList,
        applyToWholeModel = ad("applyToWholeModel").asInstanceOf[Boolean],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        isActive = ad("isActive").asInstanceOf[Boolean]
      )
    }
  }
}
