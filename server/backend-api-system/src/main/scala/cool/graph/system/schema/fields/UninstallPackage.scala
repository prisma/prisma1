package cool.graph.system.schema.fields

import cool.graph.system.mutations.UninstallPackageInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UninstallPackage {
  val inputFields =
    List(InputField("projectId", IDType, description = ""), InputField("name", StringType, description = ""))
      .asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UninstallPackageInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UninstallPackageInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        name = ad("name").asInstanceOf[String]
      )
    }
  }
}
