package com.prisma.deploy.schema.fields

import com.prisma.deploy.schema.mutations.AddProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, ListInputType, ListType, OptionInputType, StringType}

object AddProjectField {
  import ManualMarshallerHelpers._

  val inputFields = projectIdInputFields :+ InputField("secrets", OptionInputType(ListInputType(StringType)))

  implicit val fromInput = new FromInput[AddProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      AddProjectInput(
        clientMutationId = node.clientMutationId,
        ownerId = node.optionalArgAsString("ownerId"),
        name = node.requiredArgAsString("name"),
        stage = node.requiredArgAsString("stage"),
        secrets = node.optionalArgAs[Vector[String]]("secrets").getOrElse(Vector.empty)
      )
    }
  }
}
