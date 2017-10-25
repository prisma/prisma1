package cool.graph.system.schema.fields

import cool.graph.system.mutations.ExportDataInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object ExportData {
  val inputFields = List(
    InputField("projectId", StringType, description = "")
  )

  implicit val manual = new FromInput[ExportDataInput] {
    val marshaller: CoercedScalaResultMarshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node): ExportDataInput = {
      val ad = node.asInstanceOf[Map[String, Any]]

      ExportDataInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String]
      )
    }
  }
}
