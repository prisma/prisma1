package cool.graph.deploy.schema.fields

import cool.graph.deploy.schema.mutations.{DeployMutationInput, FunctionInput, HeaderInput}
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
      InputField("headers", ListInputType(headerInputType))
    )
  )

  lazy val headerInputType = InputObjectType(
    name = "HeaderInput",
    fields = List(
      InputField("name", StringType),
      InputField("value", StringType),
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
          val functionNodes = node.optionalArgAs[Vector[marshaller.Node]]("functions").getOrElse(Vector.empty)
          functionNodes.map { functionNode =>
            val headerNodes = functionNode.requiredArgAs[Vector[marshaller.Node]]("headers")
            val headers     = headerNodes.map(node => HeaderInput(node.requiredArgAsString("name"), node.requiredArgAsString("value")))
            FunctionInput(
              name = functionNode.requiredArgAs("name"),
              query = functionNode.requiredArgAs("query"),
              url = functionNode.requiredArgAs("url"),
              headers = headers
            )
          }
        }
      )
    }
  }
}
