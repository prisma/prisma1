package cool.graph.system.schema.fields

import cool.graph.system.mutations.{DeleteEnumInput, UpdateEnumInput}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField, ListInputType, OptionInputType, StringType}

object DeleteEnum {
  val inputFields =
    List(InputField("enumId", IDType, description = "")).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[DeleteEnumInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      DeleteEnumInput(
        clientMutationId = node.clientMutationId,
        enumId = node.requiredArgAsString("enumId")
      )
    }
  }
}
