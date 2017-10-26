package cool.graph.system.schema.fields

import cool.graph.system.mutations.SetProjectDatabaseInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, StringType}

object SetProjectDatabase {
  val inputFields = List(
    InputField("projectId", StringType, description = ""),
    InputField("projectDatabaseId", StringType, description = "")
  )

  implicit val manual = new FromInput[SetProjectDatabaseInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    def fromResult(node: marshaller.Node) = {
      SetProjectDatabaseInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        projectDatabaseId = node.requiredArgAsString("projectDatabaseId")
      )
    }
  }

  val trusted = TrustedMutation(inputFields, manual)
}
