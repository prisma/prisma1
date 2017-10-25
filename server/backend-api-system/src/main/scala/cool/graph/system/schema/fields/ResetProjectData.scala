package cool.graph.system.schema.fields

import cool.graph.system.mutations.{ResetProjectDataInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object ResetProjectData {
  val inputFields = List(
    InputField("projectId", StringType, description = "")
  )

  implicit val manual = new FromInput[ResetProjectDataInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      ResetProjectDataInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String]
      )
    }
  }
}
