package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.DeployMutationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeployField {
  import ManualMarshallerHelpers._

  val inputFields = List(
    InputField("projectId", StringType),
    InputField("config", StringType),
    InputField("types", StringType)
  )

  implicit val fromInput = new FromInput[DeployMutationInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      DeployMutationInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        config = node.requiredArgAsString("config"),
        types = node.requiredArgAsString("types")
      )
    }
  }
}
