package cool.graph.system.schema.fields

import cool.graph.system.mutations.DeleteFieldConstraintInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField}

object DeleteFieldConstraint {
  val inputFields = List(InputField("constraintId", IDType, description = "")).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[DeleteFieldConstraintInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      DeleteFieldConstraintInput(
        clientMutationId = node.clientMutationId,
        constraintId = node.requiredArgAsString("constraintId")
      )
    }
  }
}
