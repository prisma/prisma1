package cool.graph.system.schema.fields

import cool.graph.system.mutations.{CloneProjectInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object CloneProjectQuery {
  val inputFields = List(
    InputField("projectId", StringType, description = ""),
    InputField("name", StringType, description = ""),
    InputField("includeData", BooleanType, description = ""),
    InputField("includeMutationCallbacks", BooleanType, description = "")
  )

  implicit val manual = new FromInput[CloneProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      CloneProjectInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        name = ad("name").asInstanceOf[String],
        includeData = ad("includeData").asInstanceOf[Boolean],
        includeMutationCallbacks = ad("includeMutationCallbacks").asInstanceOf[Boolean]
      )
    }
  }
}
