package cool.graph.system.schema.fields

import cool.graph.system.mutations.{UpdateAlgoliaSyncQueryInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateAlgoliaSyncQuery {
  val inputFields = List(
    InputField("algoliaSyncQueryId", StringType, description = ""),
    InputField("indexName", StringType, description = ""),
    InputField("fragment", StringType, description = ""),
    InputField("isEnabled", BooleanType, description = "")
  )

  implicit val manual = new FromInput[UpdateAlgoliaSyncQueryInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateAlgoliaSyncQueryInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        algoliaSyncQueryId = ad("algoliaSyncQueryId").asInstanceOf[String],
        indexName = ad("indexName").asInstanceOf[String],
        fragment = ad("fragment").asInstanceOf[String],
        isEnabled = ad("isEnabled").asInstanceOf[Boolean]
      )
    }
  }
}
