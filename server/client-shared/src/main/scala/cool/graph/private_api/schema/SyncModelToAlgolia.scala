package cool.graph.private_api.schema

import cool.graph.private_api.mutations.SyncModelToAlgoliaInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField}

object SyncModelToAlgoliaMutationFields {

  val inputFields =
    List(
      InputField("modelId", IDType, description = ""),
      InputField("syncQueryId", IDType, description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[SyncModelToAlgoliaInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      SyncModelToAlgoliaInput(
        clientMutationId = node.clientMutationId,
        modelId = node.requiredArgAsString("modelId"),
        syncQueryId = node.requiredArgAsString("syncQueryId")
      )
    }
  }
}
