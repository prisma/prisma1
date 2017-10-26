package cool.graph.system.schema.types

import cool.graph.shared.models._
import cool.graph.system.{SystemUserContext}
import sangria.schema.{Field, _}
import sangria.relay._

object AuthProvider {
  lazy val NameType = EnumType(
    "AuthProviderType",
    values = List(IntegrationName.AuthProviderEmail, IntegrationName.AuthProviderDigits, IntegrationName.AuthProviderAuth0).map(authProvider =>
      EnumValue(authProvider.toString, value = authProvider))
  )

  lazy val Type: ObjectType[SystemUserContext, AuthProvider] = ObjectType(
    "AuthProvider",
    "This is a AuthProvider",
    interfaces[SystemUserContext, AuthProvider](nodeInterface),
    idField[SystemUserContext, AuthProvider] ::
      fields[SystemUserContext, AuthProvider](
      Field("type", NameType, resolve = _.value.name),
      Field("isEnabled", BooleanType, resolve = _.value.isEnabled),
      Field(
        "digits",
        OptionType(DigitsType),
        resolve = ctx =>
          ctx.value.metaInformation match {
            case Some(meta: AuthProviderDigits) if meta.isInstanceOf[AuthProviderDigits] =>
              Some(meta)
            case _ =>
              ctx.value.name match {
                case IntegrationName.AuthProviderDigits =>
                  Some(AuthProviderDigits(id = "dummy-id", consumerKey = "", consumerSecret = ""))
                case _ => None
              }
        }
      ),
      Field(
        "auth0",
        OptionType(Auth0Type),
        resolve = ctx =>
          ctx.value.metaInformation match {
            case Some(meta: AuthProviderAuth0) if meta.isInstanceOf[AuthProviderAuth0] =>
              Some(meta)
            case _ =>
              ctx.value.name match {
                case IntegrationName.AuthProviderAuth0 =>
                  Some(AuthProviderAuth0(id = "dummy-id", clientId = "", clientSecret = "", domain = ""))
                case _ => None
              }
        }
      )
    )
  )

  lazy val DigitsType: ObjectType[SystemUserContext, AuthProviderDigits] =
    ObjectType(
      "AuthProviderDigitsMeta",
      "Digits Meta Information",
      fields[SystemUserContext, AuthProviderDigits](
        Field("consumerKey", OptionType(StringType), resolve = _.value.consumerKey),
        Field("consumerSecret", OptionType(StringType), resolve = _.value.consumerSecret)
      )
    )

  lazy val Auth0Type: ObjectType[SystemUserContext, AuthProviderAuth0] =
    ObjectType(
      "AuthProviderAuth0Meta",
      "Auth0 Meta Information",
      fields[SystemUserContext, AuthProviderAuth0](
        Field("clientId", OptionType(StringType), resolve = _.value.clientId),
        Field("clientSecret", OptionType(StringType), resolve = _.value.clientSecret),
        Field("domain", OptionType(StringType), resolve = _.value.domain)
      )
    )
}
