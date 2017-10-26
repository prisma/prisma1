package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateClientPasswordInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateClientPassword {
  val inputFields = List(
    InputField("oldPassword", StringType, description = ""),
    InputField("newPassword", StringType, description = "")
  )

  implicit val manual = new FromInput[UpdateClientPasswordInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateClientPasswordInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        oldPassword = ad("oldPassword").asInstanceOf[String],
        newPassword = ad("newPassword").asInstanceOf[String]
      )
    }
  }
}
