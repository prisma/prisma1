package cool.graph.system.schema.fields

import cool.graph.TrustedInternalMutationInput
import cool.graph.system.mutations.SetProjectDatabaseInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, StringType}

case class TrustedMutation[T](originalInputFields: List[InputField[_]], fromInput: FromInput[T]) {
  val inputFields = originalInputFields :+ InputField("secret", StringType, description = "")

  implicit val manual = new FromInput[TrustedInternalMutationInput[T]] {
    val marshaller = fromInput.marshaller
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    def fromResult(node: marshaller.Node) = {
      TrustedInternalMutationInput(
        secret = node.requiredArgAsString("secret"),
        mutationInput = fromInput.fromResult(node.asInstanceOf[fromInput.marshaller.Node])
      )
    }
  }
}
