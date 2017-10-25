package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteProjectInput, UpdateModelInput, UpdateProjectInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateModel {
  val inputFields = List(
    InputField("id", StringType, description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("name", OptionInputType(StringType), description = "")
  )

  implicit val manual = new FromInput[UpdateModelInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      UpdateModelInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelId = ad("id").asInstanceOf[String],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]]),
        name = ad.get("name").flatMap(_.asInstanceOf[Option[String]]),
        fieldPositions = None
      )
    }
  }
}
