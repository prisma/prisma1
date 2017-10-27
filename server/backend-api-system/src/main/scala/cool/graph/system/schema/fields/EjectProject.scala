package cool.graph.system.schema.fields

import cool.graph.system.mutations.EjectProjectInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{IDType, InputField}

object EjectProject {
  val inputFields = List(InputField("projectId", IDType, description = "")).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[EjectProjectInput] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      EjectProjectInput(ad.get("clientMutationId").map(_.asInstanceOf[String]), ad("projectId").asInstanceOf[String])
    }
  }
}
