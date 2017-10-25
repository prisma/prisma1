package cool.graph.system.schema.fields

import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.system.mutations.AddSchemaExtensionFunctionInput
import cool.graph.system.schema.types.FunctionType
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object AddSchemaExtensionFunction {

  val inputFields =
    List(
      InputField("projectId", IDType, description = ""),
      InputField("isActive", BooleanType, description = ""),
      InputField("name", StringType, description = ""),
      InputField("schema", StringType, description = ""),
      InputField("type", FunctionType.Type, description = ""),
      InputField("webhookUrl", OptionInputType(StringType), description = ""),
      InputField("webhookHeaders", OptionInputType(StringType), description = ""),
      InputField("inlineCode", OptionInputType(StringType), description = ""),
      InputField("auth0Id", OptionInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddSchemaExtensionFunctionInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      AddSchemaExtensionFunctionInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        name = node.requiredArgAsString("name"),
        isActive = node.requiredArgAs[Boolean]("isActive"),
        schema = node.requiredArgAsString("schema"),
        functionType = node.requiredArgAs[FunctionType]("type"),
        url = node.optionalArgAsString("webhookUrl"),
        headers = node.optionalArgAsString("webhookHeaders"),
        inlineCode = node.optionalArgAsString("inlineCode"),
        auth0Id = node.optionalArgAsString("auth0Id")
      )
    }
  }
}
