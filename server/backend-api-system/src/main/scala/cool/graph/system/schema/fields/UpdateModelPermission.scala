package cool.graph.system.schema.fields

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.ModelOperation.ModelOperation
import cool.graph.shared.models.UserType.UserType
import cool.graph.system.mutations.{UpdateModelPermissionInput}
import cool.graph.system.schema.types.{Operation, Rule, UserType}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{ListInputType, OptionInputType, _}

object UpdateModelPermission {
  val inputFields = List(
    InputField("id", IDType, description = ""),
    InputField("operation", OptionInputType(Operation.Type), description = ""),
    InputField("userType", OptionInputType(UserType.Type), description = ""),
    InputField("rule", OptionInputType(Rule.Type), description = ""),
    InputField("ruleName", OptionInputType(StringType), description = ""),
    InputField("ruleGraphQuery", OptionInputType(StringType), description = ""),
    InputField("ruleWebhookUrl", OptionInputType(StringType), description = ""),
    InputField("fieldIds", OptionInputType(ListInputType(StringType)), description = ""),
    InputField("applyToWholeModel", OptionInputType(BooleanType), description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("isActive", OptionInputType(BooleanType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateModelPermissionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateModelPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        id = ad("id").asInstanceOf[String],
        operation = ad.get("operation").flatMap(_.asInstanceOf[Option[ModelOperation]]),
        userType = ad.get("userType").flatMap(_.asInstanceOf[Option[UserType]]),
        rule = ad.get("rule").flatMap(_.asInstanceOf[Option[CustomRule]]),
        ruleName = ad.get("ruleName").flatMap(_.asInstanceOf[Option[String]]),
        ruleGraphQuery = ad.get("ruleGraphQuery").flatMap(_.asInstanceOf[Option[String]]),
        ruleWebhookUrl = ad.get("ruleWebhookUrl").flatMap(_.asInstanceOf[Option[String]]),
        fieldIds = ad.get("fieldIds").flatMap(_.asInstanceOf[Option[Vector[String]]].map(_.toList)),
        applyToWholeModel = ad.get("applyToWholeModel").flatMap(_.asInstanceOf[Option[Boolean]]),
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        isActive = ad.get("isActive").flatMap(_.asInstanceOf[Option[Boolean]])
      )
    }
  }
}
