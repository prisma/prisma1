package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteFieldInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteField {
  val inputFields = List(
    InputField("fieldId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteFieldInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteFieldInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        fieldId = ad("fieldId").asInstanceOf[String]
      )
    }
  }
}
