package cool.graph.relay.auth.integrations

import cool.graph.DataItem
import cool.graph.client.{UserContext$, UserContext}
import sangria.schema.{Field, OptionType, _}

case class IntegrationSigninData(token: String, user: DataItem)

object SigninIntegration {
  def fieldType(userFieldType: ObjectType[UserContext, DataItem]): ObjectType[UserContext, Option[IntegrationSigninData]] =
    ObjectType(
      "SigninPayload",
      description = "In case signin was successful contains the user and a token or null otherwise",
      fields = fields[UserContext, Option[IntegrationSigninData]](
        Field(name = "token", fieldType = OptionType(StringType), resolve = _.value.map(_.token)),
        Field(name = "user", fieldType = OptionType(userFieldType), resolve = _.value.map(_.user))
      )
    )
}
