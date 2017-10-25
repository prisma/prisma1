package cool.graph.system.schema.fields

import cool.graph.system.mutations.{ResetClientPasswordInput, UpdateClientPasswordInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object ResetClientPassword {
  val inputFields = List(
    InputField("resetPasswordToken", StringType, description = ""),
    InputField("newPassword", StringType, description = "")
  )

  implicit val manual = new FromInput[ResetClientPasswordInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      ResetClientPasswordInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        resetPasswordToken = ad("resetPasswordToken").asInstanceOf[String],
        newPassword = ad("newPassword").asInstanceOf[String]
      )
    }
  }
}
