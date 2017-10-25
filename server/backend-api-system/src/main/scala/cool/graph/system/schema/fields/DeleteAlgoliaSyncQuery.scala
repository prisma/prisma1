package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteAlgoliaSyncQueryInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteAlgoliaSyncQuery {
  val inputFields = List(
    InputField("algoliaSyncQueryId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteAlgoliaSyncQueryInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteAlgoliaSyncQueryInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        algoliaSyncQueryId = ad("algoliaSyncQueryId").asInstanceOf[String]
      )
    }
  }
}
