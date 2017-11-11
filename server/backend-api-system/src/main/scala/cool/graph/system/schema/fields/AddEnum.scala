package cool.graph.system.schema.fields

import cool.graph.system.mutations.{AddEnumInput, AddModelInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField, ListInputType, OptionInputType, StringType}

object AddEnum {
  val inputFields =
    List(
      InputField("projectId", IDType, description = ""),
      InputField("name", StringType, description = ""),
      InputField("values", ListInputType(StringType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddEnumInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      AddEnumInput(
        clientMutationId = node.clientMutationId,
        projectId = node.requiredArgAsString("projectId"),
        name = node.requiredArgAsString("name"),
        values = node.requiredArgAs[Seq[String]]("values")
      )
    }
  }
}
