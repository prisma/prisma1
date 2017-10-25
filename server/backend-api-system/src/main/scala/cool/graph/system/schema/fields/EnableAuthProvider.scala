package cool.graph.system.schema.fields

import cool.graph.system.mutations.EnableAuthProviderInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object EnableAuthProvider {
  lazy val DigitsType = InputObjectType(
    name = "AuthProviderDigitsMetaInput",
    fields = List(
      InputField("consumerKey", StringType),
      InputField("consumerSecret", StringType)
    )
  )
  lazy val Auth0Type = InputObjectType(
    name = "AuthProviderAuth0MetaInput",
    fields = List(
      InputField("clientId", StringType),
      InputField("clientSecret", StringType),
      InputField("domain", StringType)
    )
  )

  val inputFields = List(
    InputField("id", IDType, description = ""),
    InputField("isEnabled", BooleanType, description = ""),
    InputField("digits", OptionInputType(DigitsType)),
    InputField("auth0", OptionInputType(Auth0Type))
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[EnableAuthProviderInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      EnableAuthProviderInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        id = ad("id").asInstanceOf[String],
        isEnabled = ad("isEnabled").asInstanceOf[Boolean],
//        authProvider = ad("type").asInstanceOf[IntegrationName],
        digitsConsumerKey = ad
          .get("digits")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]].map(_("consumerKey")))
          .map(_.asInstanceOf[String]),
        digitsConsumerSecret = ad
          .get("digits")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]].map(_("consumerSecret")))
          .map(_.asInstanceOf[String]),
        auth0ClientId = ad
          .get("auth0")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]].map(_("clientId")))
          .map(_.asInstanceOf[String]),
        auth0ClientSecret = ad
          .get("auth0")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]].map(_("clientSecret")))
          .map(_.asInstanceOf[String]),
        auth0Domain = ad
          .get("auth0")
          .flatMap(_.asInstanceOf[Option[Map[String, Any]]].map(_("domain")))
          .map(_.asInstanceOf[String])
      )
    }
  }
}
