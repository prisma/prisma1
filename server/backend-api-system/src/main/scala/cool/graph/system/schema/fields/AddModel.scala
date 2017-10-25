package cool.graph.system.schema.fields

import cool.graph.system.mutations.AddModelInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object AddModel {
  val inputFields = List(
    InputField("projectId", IDType, description = ""),
    InputField("modelName", StringType, description = ""),
    InputField("description", OptionInputType(StringType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddModelInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddModelInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        modelName = ad("modelName").asInstanceOf[String],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        fieldPositions = None
      )
    }
  }
}
