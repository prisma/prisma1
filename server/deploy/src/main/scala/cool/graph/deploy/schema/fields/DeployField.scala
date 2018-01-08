package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.{DeployMutationInput, FunctionInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeployField {
  import ManualMarshallerHelpers._

  lazy val inputFields = projectIdInputFields ++ List(
    InputField("types", StringType),
    InputField("dryRun", OptionInputType(BooleanType)),
    InputField("secrets", OptionInputType(ListInputType(StringType))),
    InputField("functions", OptionInputType(ListInputType(functionInputType)))
  )

  lazy val functionInputType = InputObjectType(
    name = "FunctionInput",
    fields = List(
      InputField("name", StringType),
      InputField("query", StringType),
      InputField("url", StringType),
      InputField("headers", StringType)
    )
  )

  implicit val fromInput = new FromInput[DeployMutationInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      DeployMutationInput(
        clientMutationId = node.clientMutationId,
        projectId = node.projectId,
        types = node.requiredArgAsString("types"),
        dryRun = node.optionalArgAsBoolean("dryRun"),
        secrets = node.optionalArgAs[Vector[String]]("secrets").getOrElse(Vector.empty),
        functions = {
          val asMaps = node.optionalArgAs[Vector[Map[String, Any]]]("functions").getOrElse(Vector.empty)
          asMaps.map { map =>
            FunctionInput(
              name = map.requiredArgAs("name"),
              query = map.requiredArgAs("query"),
              url = map.requiredArgAs("url"),
              headers = map.requiredArgAs("headers")
            )
          }
        }
      )
    }
  }
}
