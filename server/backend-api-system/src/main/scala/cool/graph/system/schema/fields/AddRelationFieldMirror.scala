package cool.graph.system.schema.fields

import cool.graph.system.mutations.AddRelationFieldMirrorInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object AddRelationFieldMirror {

  val inputFields =
    List(InputField("fieldId", IDType, description = ""), InputField("relationId", IDType, description = ""))
      .asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddRelationFieldMirrorInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddRelationFieldMirrorInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        fieldId = ad("fieldId").asInstanceOf[String],
        relationId = ad("relationId").asInstanceOf[String]
      )
    }
  }
}
