package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateClientInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateClient {
  val inputFields = List(
    InputField("name", OptionInputType(StringType), description = ""),
    InputField("email", OptionInputType(StringType), description = "")
  )

  implicit val manual = new FromInput[UpdateClientInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateClientInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        name = ad.get("name").flatMap(_.asInstanceOf[Option[String]]),
        email = ad.get("email").flatMap(_.asInstanceOf[Option[String]])
      )
    }
  }
}
