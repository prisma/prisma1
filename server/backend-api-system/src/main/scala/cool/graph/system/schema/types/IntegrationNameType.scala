package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object IntegrationNameType {
  val Type = EnumType(
    "IntegrationNameType",
    values = List(
      EnumValue("AUTH_PROVIDER_AUTH0", value = models.IntegrationName.AuthProviderAuth0),
      EnumValue("AUTH_PROVIDER_DIGITS", value = models.IntegrationName.AuthProviderDigits),
      EnumValue("AUTH_PROVIDER_EMAIL", value = models.IntegrationName.AuthProviderEmail),
      EnumValue("SEARCH_PROVIDER_ALGOLIA", value = models.IntegrationName.SearchProviderAlgolia)
    )
  )
}
