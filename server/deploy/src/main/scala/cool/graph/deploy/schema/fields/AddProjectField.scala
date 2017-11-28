package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.AddProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, OptionInputType, StringType}

object AddProjectField {
  import ManualMarshallerHelpers._

  val inputFields = List(
    InputField("name", StringType),
    InputField("alias", OptionInputType(StringType))
  )

  implicit val fromInput = new FromInput[AddProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      AddProjectInput(
        clientMutationId = node.clientMutationId,
        name = node.requiredArgAsString("name"),
        alias = node.optionalArgAsString("alias")
      )
    }
  }
}
