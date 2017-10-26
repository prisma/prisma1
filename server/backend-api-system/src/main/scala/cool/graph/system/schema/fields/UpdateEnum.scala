package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateEnumInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField, ListInputType, OptionInputType, StringType}

object UpdateEnum {
  val inputFields =
    List(
      InputField("enumId", IDType, description = ""),
      InputField("name", OptionInputType(StringType), description = ""),
      InputField("values", OptionInputType(ListInputType(StringType)), description = ""),
      InputField("migrationValue", OptionInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateEnumInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      UpdateEnumInput(
        clientMutationId = node.clientMutationId,
        enumId = node.requiredArgAsString("enumId"),
        name = node.optionalArgAsString("name"),
        values = node.optionalArgAs[Seq[String]]("values"),
        migrationValue = node.optionalArgAsString("migrationValue")
      )
    }
  }
}
