package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.DeleteProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}

object DeleteProjectField {
  import ManualMarshallerHelpers._

  val inputFields = projectIdInputFields

  implicit val fromInput = new FromInput[DeleteProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      DeleteProjectInput(
        clientMutationId = node.clientMutationId,
        name = node.requiredArgAsString("name"),
        stage = node.requiredArgAsString("stage")
      )
    }
  }
}
