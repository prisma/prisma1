package cool.graph.system.schema.fields

import cool.graph.shared.models.TypeIdentifier
import cool.graph.system.mutations.AddFieldInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object AddField {
  val inputFields = List(
    InputField("modelId", IDType, description = ""),
    InputField("name", StringType, description = ""),
    InputField("typeIdentifier", StringType, description = ""),
    InputField("isRequired", BooleanType, description = ""),
    InputField("isList", BooleanType, description = ""),
    InputField("isUnique", BooleanType, description = ""),
    InputField("relationId", OptionInputType(StringType), description = ""),
    InputField("enumId", OptionInputType(IDType), description = ""),
    InputField("defaultValue", OptionInputType(StringType), description = ""),
    InputField("migrationValue", OptionInputType(StringType), description = ""),
    InputField("description", OptionInputType(StringType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddFieldInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node): AddFieldInput = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddFieldInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        modelId = ad("modelId").asInstanceOf[String],
        name = ad("name").asInstanceOf[String],
        typeIdentifier = TypeIdentifier.withName(ad("typeIdentifier").asInstanceOf[String]),
        isRequired = ad("isRequired").asInstanceOf[Boolean],
        isList = ad("isList").asInstanceOf[Boolean],
        isUnique = ad("isUnique").asInstanceOf[Boolean],
        relationId = ad.get("relationId").flatMap(_.asInstanceOf[Option[String]]),
        enumId = ad.get("enumId").flatMap(_.asInstanceOf[Option[String]]),
        defaultValue = ad.get("defaultValue").flatMap(_.asInstanceOf[Option[String]]),
        migrationValue = ad.get("migrationValue").flatMap(_.asInstanceOf[Option[String]]),
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]])
      )
    }
  }
}
