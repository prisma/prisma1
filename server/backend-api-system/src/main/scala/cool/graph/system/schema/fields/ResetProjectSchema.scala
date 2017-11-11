package cool.graph.system.schema.fields

import cool.graph.system.mutations.ResetProjectSchemaInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object ResetProjectSchema {
  val inputFields = List(
    InputField("projectId", StringType, description = "")
  )

  implicit val manual = new FromInput[ResetProjectSchemaInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      ResetProjectSchemaInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String]
      )
    }
  }
}
