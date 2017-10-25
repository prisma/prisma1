package cool.graph.system.schema.fields

import cool.graph.system.mutations.AuthenticateCustomerInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object AuthenticateCustomer {
  val inputFields = List(
    InputField("auth0IdToken", StringType, description = "")
  )

  implicit val manual = new FromInput[AuthenticateCustomerInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AuthenticateCustomerInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        auth0IdToken = ad("auth0IdToken").asInstanceOf[String]
      )
    }
  }
}
