package cool.graph.system.schema.fields

import cool.graph.system.mutations.TransferOwnershipInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object TransferOwnership {
  val inputFields =
    List(InputField("projectId", IDType, description = ""), InputField("email", StringType, description = ""))
      .asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[TransferOwnershipInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      TransferOwnershipInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        email = ad("email").asInstanceOf[String]
      )
    }
  }
}
