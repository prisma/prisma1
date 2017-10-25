package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteModelInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteModel {
  val inputFields = List(
    InputField("modelId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteModelInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteModelInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelId = ad("modelId").asInstanceOf[String]
      )
    }
  }
}
