package cool.graph.system.schema.fields

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.system.mutations.AddRequestPipelineMutationFunctionInput
import cool.graph.system.schema.types.{FunctionBinding, FunctionType, RequestPipelineMutationOperation}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField, OptionInputType, StringType}

object AddRequestPipelineMutationFunction {
  val inputFields: List[InputField[Any]] =
    List(
      InputField("projectId", IDType, description = ""),
      InputField("name", StringType, description = ""),
      InputField("isActive", sangria.schema.BooleanType, description = ""),
      InputField("binding", FunctionBinding.Type, description = ""),
      InputField("modelId", StringType, description = ""),
      InputField("operation", RequestPipelineMutationOperation.Type, description = ""),
      InputField("type", FunctionType.Type, description = ""),
      InputField("webhookUrl", OptionInputType(StringType), description = ""),
      InputField("webhookHeaders", OptionInputType(StringType), description = ""),
      InputField("inlineCode", OptionInputType(StringType), description = ""),
      InputField("auth0Id", OptionInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddRequestPipelineMutationFunctionInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller: CoercedScalaResultMarshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      AddRequestPipelineMutationFunctionInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        name = node.requiredArgAsString("name"),
        isActive = node.requiredArgAs[Boolean]("isActive"),
        binding = node.requiredArgAs[FunctionBinding]("binding"),
        modelId = node.requiredArgAs[String]("modelId"),
        operation = node.requiredArgAs[RequestPipelineOperation]("operation"),
        functionType = node.requiredArgAs[FunctionType]("type"),
        webhookUrl = node.optionalArgAsString("webhookUrl"),
        headers = node.optionalArgAsString("webhookHeaders"),
        inlineCode = node.optionalArgAsString("inlineCode"),
        auth0Id = node.optionalArgAsString("auth0Id")
      )
    }
  }
}
