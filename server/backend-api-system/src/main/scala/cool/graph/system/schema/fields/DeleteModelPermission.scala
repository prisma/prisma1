package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteModelPermissionInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteModelPermission {
  val inputFields = List(
    InputField("modelPermissionId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteModelPermissionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteModelPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelPermissionId = ad("modelPermissionId").asInstanceOf[String]
      )
    }
  }
}
