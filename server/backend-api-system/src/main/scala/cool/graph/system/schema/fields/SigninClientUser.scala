package cool.graph.system.schema.fields

import cool.graph.system.mutations._
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object SigninClientUser {

  val inputFields =
    List(InputField("projectId", IDType, description = ""), InputField("clientUserId", IDType, description = ""))
      .asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[SigninClientUserInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      SigninClientUserInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        clientUserId = ad("clientUserId").asInstanceOf[String]
      )
    }
  }
}
