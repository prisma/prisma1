package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteModelInput, DeleteRelationFieldMirrorInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteRelationFieldMirror {
  val inputFields = List(
    InputField("relationFieldMirrorId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteRelationFieldMirrorInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteRelationFieldMirrorInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        relationFieldMirrorId = ad("relationFieldMirrorId").asInstanceOf[String]
      )
    }
  }
}
