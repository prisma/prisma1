package com.prisma.deploy.schema.fields

import com.prisma.deploy.schema.mutations.SetCloudSecretMutationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, OptionInputType, StringType}

object SetCloudSecretField {
  import ManualMarshallerHelpers._

  val inputFields = List(InputField("secret", OptionInputType(StringType)))

  implicit val fromInput = new FromInput[SetCloudSecretMutationInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      SetCloudSecretMutationInput(
        clientMutationId = node.clientMutationId,
        cloudSecret = node.optionalArgAsString("secret")
      )
    }
  }
}
