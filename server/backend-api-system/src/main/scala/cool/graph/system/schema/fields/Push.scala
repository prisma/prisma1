package cool.graph.system.schema.fields

import cool.graph.system.mutations.{MigrateSchemaInput, PushInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object Push {
  val inputFields = List(
    InputField("projectId", StringType, description = ""),
    InputField("version", IntType, description = ""),
    InputField("config", StringType, description = ""),
    InputField("isDryRun", BooleanType, description = "If set to false the migration is not performed."),
    InputField("force", OptionInputType(BooleanType), description = "If set to false the migration will fail if data would be lost. Defaults to false.")
  )

  implicit val fromInput = new FromInput[PushInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      PushInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        version = ad("version").asInstanceOf[Int],
        config = ad("config").asInstanceOf[String],
        isDryRun = ad("isDryRun").asInstanceOf[Boolean],
        force = ad.get("force").flatMap(_.asInstanceOf[Option[Boolean]]).getOrElse(false)
      )
    }
  }
}
