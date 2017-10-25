package cool.graph.system.schema.fields

import cool.graph.system.mutations.InstallPackageInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object InstallPackage {
  val inputFields =
    List(InputField("projectId", IDType, description = ""), InputField("definition", StringType, description = ""))
      .asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[InstallPackageInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      InstallPackageInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        definition = ad("definition").asInstanceOf[String]
      )
    }
  }
}
