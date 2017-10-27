package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateFieldInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object UpdateField {
  val inputFields = List(
    InputField("id", StringType, description = ""),
    InputField("defaultValue", OptionInputType(StringType), description = ""),
    InputField("migrationValue", OptionInputType(StringType), description = ""),
    InputField("description", OptionInputType(StringType), description = ""),
    InputField("name", OptionInputType(StringType), description = ""),
    InputField("typeIdentifier", OptionInputType(StringType), description = ""),
    InputField("isUnique", OptionInputType(BooleanType), description = ""),
    InputField("isRequired", OptionInputType(BooleanType), description = ""),
    InputField("isList", OptionInputType(BooleanType), description = ""),
    InputField("enumId", OptionInputType(IDType), description = "")
  )

  implicit val manual = new FromInput[UpdateFieldInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      UpdateFieldInput(
        clientMutationId = node.clientMutationId,
        fieldId = node.requiredArgAsString("id"),
        defaultValue = node.optionalOptionalArgAsString("defaultValue"),
        migrationValue = node.optionalArgAsString("migrationValue"),
        description = node.optionalArgAsString("description"),
        name = node.optionalArgAsString("name"),
        typeIdentifier = node.optionalArgAsString("typeIdentifier"),
        isUnique = node.optionalArgAs[Boolean]("isUnique"),
        isRequired = node.optionalArgAs[Boolean]("isRequired"),
        isList = node.optionalArgAs[Boolean]("isList"),
        enumId = node.optionalArgAs[String]("enumId")
      )
    }
  }
}
