package cool.graph.system.schema.fields

import cool.graph.system.mutations.MigrateSchemaInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object MigrateSchema {
  val inputFields = List(
    InputField("newSchema", StringType, description = ""),
    InputField("isDryRun", BooleanType, description = "If set to false the migration is not performed."),
    InputField("force", OptionInputType(BooleanType), description = "If set to false the migration will fail if data would be lost. Defaults to false.")
  )

  implicit val fromInput = new FromInput[MigrateSchemaInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      MigrateSchemaInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        newSchema = ad("newSchema").asInstanceOf[String],
        isDryRun = ad("isDryRun").asInstanceOf[Boolean],
        force = ad.get("force").flatMap(_.asInstanceOf[Option[Boolean]]).getOrElse(false)
      )
    }
  }
}
