package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteCustomerInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteCustomer {
  val inputFields = List(
    InputField("customerId", StringType, description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[DeleteCustomerInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteCustomerInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        customerId = ad("customerId").asInstanceOf[String]
      )
    }
  }
}
