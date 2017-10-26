package cool.graph.system.schema.fields

import cool.graph.system.mutations.{CreateRootTokenInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object CreateRootToken {

  val inputFields = List(
    InputField("projectId", IDType, description = ""),
    InputField("name", StringType, description = ""),
    InputField("description", OptionInputType(StringType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[CreateRootTokenInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      CreateRootTokenInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        name = ad("name").asInstanceOf[String],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]])
      )
    }
  }
}
