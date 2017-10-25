package cool.graph.system.schema.fields

import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.system.mutations.UpdateServerSideSubscriptionFunctionInput
import cool.graph.system.schema.types.FunctionType
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{BooleanType, IDType, InputField, OptionInputType, StringType}

object UpdateServerSideSubscriptionFunction {
  val inputFields =
    List(
      InputField("functionId", IDType, description = ""),
      InputField("name", OptionInputType(StringType), description = ""),
      InputField("isActive", OptionInputType(BooleanType), description = ""),
      InputField("query", OptionInputType(StringType), description = ""),
      InputField("type", OptionInputType(FunctionType.Type), description = ""),
      InputField("webhookUrl", OptionInputType(StringType), description = ""),
      InputField("webhookHeaders", OptionInputType(StringType), description = ""),
      InputField("inlineCode", OptionInputType(StringType), description = ""),
      InputField("auth0Id", OptionInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateServerSideSubscriptionFunctionInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      UpdateServerSideSubscriptionFunctionInput(
        clientMutationId = node.clientMutationId,
        functionId = node.requiredArgAsString("functionId"),
        name = node.optionalArgAsString("name"),
        isActive = node.optionalArgAs[Boolean]("isActive"),
        query = node.optionalArgAsString("query"),
        functionType = node.optionalArgAs[FunctionType]("type"),
        webhookUrl = node.optionalArgAsString("webhookUrl"),
        headers = node.optionalArgAsString("webhookHeaders"),
        inlineCode = node.optionalArgAsString("inlineCode"),
        auth0Id = node.optionalArgAsString("auth0Id")
      )
    }
  }
}
