package cool.graph.system.schema.fields

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.system.mutations.UpdateRequestPipelineMutationFunctionInput
import cool.graph.system.schema.types.{FunctionBinding, FunctionType, RequestPipelineMutationOperation}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField, OptionInputType, StringType}

object UpdateRequestPipelineMutationFunction {
  val inputFields =
    List(
      InputField("functionId", IDType, description = ""),
      InputField("name", OptionInputType(StringType), description = ""),
      InputField("isActive", OptionInputType(sangria.schema.BooleanType), description = ""),
      InputField("operation", OptionInputType(RequestPipelineMutationOperation.Type), description = ""),
      InputField("binding", OptionInputType(FunctionBinding.Type), description = ""),
      InputField("modelId", OptionInputType(StringType), description = ""),
      InputField("type", OptionInputType(FunctionType.Type), description = ""),
      InputField("webhookUrl", OptionInputType(StringType), description = ""),
      InputField("webhookHeaders", OptionInputType(StringType), description = ""),
      InputField("inlineCode", OptionInputType(StringType), description = ""),
      InputField("auth0Id", OptionInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateRequestPipelineMutationFunctionInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      UpdateRequestPipelineMutationFunctionInput(
        clientMutationId = node.clientMutationId,
        functionId = node.requiredArgAsString("functionId"),
        name = node.optionalArgAsString("name"),
        binding = node.optionalArgAs[FunctionBinding]("binding"),
        modelId = node.optionalArgAs[String]("modelId"),
        isActive = node.optionalArgAs[Boolean]("isActive"),
        operation = node.optionalArgAs[RequestPipelineOperation]("operation"),
        functionType = node.optionalArgAs[FunctionType]("type"),
        webhookUrl = node.optionalArgAsString("webhookUrl"),
        headers = node.optionalArgAsString("webhookHeaders"),
        inlineCode = node.optionalArgAsString("inlineCode"),
        auth0Id = node.optionalArgAsString("auth0Id")
      )
    }
  }
}
