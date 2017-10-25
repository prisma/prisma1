package cool.graph.system.schema.fields

import cool.graph.system.mutations.{SetFeatureToggleInput, UpdateProjectInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{BooleanType, InputField, OptionInputType, StringType}

object SetFeatureToggle {
  val inputFields = List(
    InputField("projectId", StringType, description = ""),
    InputField("name", StringType, description = ""),
    InputField("isEnabled", BooleanType, description = "")
  )

  implicit val manual = new FromInput[SetFeatureToggleInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    def fromResult(node: marshaller.Node) = {
      SetFeatureToggleInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        name = node.requiredArgAsString("name"),
        isEnabled = node.requiredArgAs[Boolean]("isEnabled")
      )
    }
  }

  val trusted = TrustedMutation(inputFields, manual)
}
