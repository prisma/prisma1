package com.prisma.deploy.schema.fields

import com.prisma.deploy.schema.mutations.SetCloudSecretMutationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputField, StringType}

object SetCloudSecretField {
  import ManualMarshallerHelpers._

  val inputFields = List(InputField("secret", StringType))

  implicit val fromInput = new FromInput[SetCloudSecretMutationInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      SetCloudSecretMutationInput(
        clientMutationId = node.clientMutationId,
        cloudSecret = None
      )
    }
  }
}
