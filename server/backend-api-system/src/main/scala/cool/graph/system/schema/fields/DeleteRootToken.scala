package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteRootTokenInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteRootToken {
  val inputFields = List(
    InputField("permanentAuthTokenId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteRootTokenInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteRootTokenInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        rootTokenId = ad("permanentAuthTokenId").asInstanceOf[String]
      )
    }
  }
}
