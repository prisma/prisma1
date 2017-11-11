package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateProject {
  val inputFields = List(
    InputField("id", StringType, description = ""),
    InputField("name", OptionInputType(StringType), description = ""),
    InputField("alias", OptionInputType(StringType), description = ""),
    InputField("webhookUrl", OptionInputType(StringType), description = ""),
    InputField("allowQueries", OptionInputType(BooleanType), description = ""),
    InputField("allowMutations", OptionInputType(BooleanType), description = "")
  )

  implicit val manual = new FromInput[UpdateProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    def fromResult(node: marshaller.Node) = {
      UpdateProjectInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("id"),
        name = node.optionalArgAsString("name"),
        alias = node.optionalArgAsString("alias"),
        webhookUrl = node.optionalArgAsString("webhookUrl"),
        allowQueries = node.optionalArgAs[Boolean]("allowQueries"),
        allowMutations = node.optionalArgAs[Boolean]("allowMutations")
      )
    }
  }

  val trusted = TrustedMutation(inputFields, manual)
}
