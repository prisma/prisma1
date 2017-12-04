package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.AddProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}

object AddProjectField {
  import ManualMarshallerHelpers._

  val inputFields = projectIdInputFields

  implicit val fromInput = new FromInput[AddProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      AddProjectInput(
        clientMutationId = node.clientMutationId,
        projectId = node.projectId
      )
    }
  }
}
