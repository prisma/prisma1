package cool.graph.system.schema.fields

import cool.graph.shared.models
import cool.graph.system.mutations.AddProjectInput
import cool.graph.system.schema.types.Region
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object AddProject {
  val inputFields = List(
    InputField("name", StringType, description = ""),
    InputField("alias", OptionInputType(StringType), description = ""),
    InputField("webhookUrl", OptionInputType(StringType), description = ""),
    InputField("schema", OptionInputType(StringType), description = ""),
    InputField("region", OptionInputType(Region.Type), description = ""),
    InputField("config", OptionInputType(StringType), description = "")
  )

  implicit val manual = new FromInput[AddProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddProjectInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        name = ad("name").asInstanceOf[String],
        alias = ad.get("alias").flatMap(_.asInstanceOf[Option[String]]),
        webhookUrl = ad.get("webhookUrl").flatMap(_.asInstanceOf[Option[String]]),
        schema = ad.get("schema").flatMap(_.asInstanceOf[Option[String]]),
        region = ad.get("region").flatMap(_.asInstanceOf[Option[models.Region.Region]]).getOrElse(models.Region.EU_WEST_1),
        projectDatabaseId = None,
        config = ad.get("config").flatMap(_.asInstanceOf[Option[String]])
      )
    }
  }
}
