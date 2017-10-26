package cool.graph.system.schema.fields

import cool.graph.system.mutations._
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema._

object GenerateUserToken {

  val inputFields =
    List(
      InputField("pat", StringType, description = ""),
      InputField("projectId", IDType, description = ""),
      InputField("userId", IDType, description = ""),
      InputField("modelName", IDType, description = ""),
      InputField("expirationInSeconds", OptionInputType(IntType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[GenerateUserTokenInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      GenerateUserTokenInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        pat = ad("pat").asInstanceOf[String],
        projectId = ad("projectId").asInstanceOf[String],
        userId = ad("userId").asInstanceOf[String],
        modelName = ad("modelName").asInstanceOf[String],
        expirationInSeconds = ad.get("expirationInSeconds").flatMap(_.asInstanceOf[Option[Int]])
      )
    }
  }
}

object GenerateNodeToken {

  val inputFields =
    List(
      InputField("rootToken", StringType, description = ""),
      InputField("serviceId", IDType, description = ""),
      InputField("nodeId", IDType, description = ""),
      InputField("modelName", IDType, description = ""),
      InputField("expirationInSeconds", OptionInputType(IntType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[GenerateUserTokenInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      GenerateUserTokenInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        pat = ad("rootToken").asInstanceOf[String],
        projectId = ad("serviceId").asInstanceOf[String],
        userId = ad("nodeId").asInstanceOf[String],
        modelName = ad("modelName").asInstanceOf[String],
        expirationInSeconds = ad.get("expirationInSeconds").flatMap(_.asInstanceOf[Option[Int]])
      )
    }
  }
}
