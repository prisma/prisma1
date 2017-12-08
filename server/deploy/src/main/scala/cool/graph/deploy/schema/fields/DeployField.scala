package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.DeployMutationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeployField {
  import ManualMarshallerHelpers._

  val inputFields = projectIdInputFields ++ List(
    InputField("types", StringType),
    InputField("dryRun", OptionInputType(BooleanType)),
    InputField("secrets", OptionInputType(ListInputType(StringType)))
  )

  implicit val fromInput = new FromInput[DeployMutationInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      DeployMutationInput(
        clientMutationId = node.clientMutationId,
        projectId = node.projectId,
        types = node.requiredArgAsString("types"),
        dryRun = node.optionalArgAsBoolean("dryRun"),
        secrets = node.optionalArgAs[Vector[String]]("secrets").getOrElse(Vector.empty)
      )
    }
  }
}
