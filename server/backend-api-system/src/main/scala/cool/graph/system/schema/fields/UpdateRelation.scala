package cool.graph.system.schema.fields

import cool.graph.system.mutations.UpdateRelationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object UpdateRelation {
  val inputFields = List(
    InputField("id", IDType, description = ""),
    InputField("leftModelId", OptionInputType(IDType), description = ""),
    InputField("rightModelId", OptionInputType(IDType), description = ""),
    InputField("fieldOnLeftModelName", OptionInputType(StringType), description = ""),
    InputField("fieldOnRightModelName", OptionInputType(StringType), description = ""),
    InputField("fieldOnLeftModelIsList", OptionInputType(BooleanType), description = ""),
    InputField("fieldOnRightModelIsList", OptionInputType(BooleanType), description = ""),
    InputField("fieldOnLeftModelIsRequired", OptionInputType(BooleanType), description = ""),
    InputField("fieldOnRightModelIsRequired", OptionInputType(BooleanType), description = ""),
    InputField("name", OptionInputType(StringType), description = ""),
    InputField("description", OptionInputType(StringType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateRelationInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {

      UpdateRelationInput(
        clientMutationId = node.optionalArgAsString("clientMutationId"),
        id = node.requiredArgAsString("id"),
        leftModelId = node.optionalArgAsString("leftModelId"),
        rightModelId = node.optionalArgAsString("rightModelId"),
        fieldOnLeftModelName = node.optionalArgAsString("fieldOnLeftModelName"),
        fieldOnRightModelName = node.optionalArgAsString("fieldOnRightModelName"),
        fieldOnLeftModelIsList = node.optionalArgAsBoolean("fieldOnLeftModelIsList"),
        fieldOnRightModelIsList = node.optionalArgAsBoolean("fieldOnRightModelIsList"),
        fieldOnLeftModelIsRequired = node.optionalArgAsBoolean("fieldOnLeftModelIsRequired"),
        fieldOnRightModelIsRequired = node.optionalArgAsBoolean("fieldOnRightModelIsRequired"),
        name = node.optionalArgAsString("name"),
        description = node.optionalArgAsString("description")
      )
    }
  }
}
