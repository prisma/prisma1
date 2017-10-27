package cool.graph.system.schema.fields

import cool.graph.system.mutations.AddAlgoliaSyncQueryInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object AddAlgoliaSyncQuery {
  val inputFields = List(
    InputField("modelId", StringType, description = ""),
    InputField("indexName", StringType, description = ""),
    InputField("fragment", StringType, description = "")
  )

  implicit val manual = new FromInput[AddAlgoliaSyncQueryInput] {
    val marshaller: CoercedScalaResultMarshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node): AddAlgoliaSyncQueryInput = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddAlgoliaSyncQueryInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelId = ad("modelId").asInstanceOf[String],
        indexName = ad("indexName").asInstanceOf[String],
        fragment = ad("fragment").asInstanceOf[String]
      )
    }
  }
}
