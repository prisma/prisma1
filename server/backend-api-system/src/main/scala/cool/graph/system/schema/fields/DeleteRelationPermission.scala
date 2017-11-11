package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteModelPermissionInput, DeleteRelationPermissionInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteRelationPermission {
  val inputFields = List(
    InputField("relationPermissionId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteRelationPermissionInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteRelationPermissionInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        relationPermissionId = ad("relationPermissionId").asInstanceOf[String]
      )
    }
  }
}
