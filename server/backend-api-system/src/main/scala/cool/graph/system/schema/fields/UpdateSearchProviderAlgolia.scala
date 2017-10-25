package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateSearchProviderAlgoliaInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateSearchProviderAlgolia {
  val inputFields = List(
    // Can probably remove projectId
    InputField("projectId", StringType, description = ""),
    InputField("applicationId", StringType, description = ""),
    InputField("apiKey", StringType, description = ""),
    InputField("isEnabled", BooleanType, description = "")
  )

  implicit val manual = new FromInput[UpdateSearchProviderAlgoliaInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateSearchProviderAlgoliaInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        applicationId = ad("applicationId").asInstanceOf[String],
        apiKey = ad("apiKey").asInstanceOf[String],
        isEnabled = ad("isEnabled").asInstanceOf[Boolean]
      )
    }
  }
}
