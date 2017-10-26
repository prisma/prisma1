package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteFunctionInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteFunction {
  val inputFields = List(
    InputField("functionId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteFunctionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteFunctionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        functionId = ad("functionId").asInstanceOf[String]
      )
    }
  }
}
