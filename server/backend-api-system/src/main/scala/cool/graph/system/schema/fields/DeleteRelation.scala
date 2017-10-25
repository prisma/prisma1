package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteModelInput, DeleteRelationInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object DeleteRelation {
  val inputFields = List(
    InputField("relationId", StringType, description = "")
  )

  implicit val manual = new FromInput[DeleteRelationInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      DeleteRelationInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        relationId = ad("relationId").asInstanceOf[String]
      )
    }
  }
}
