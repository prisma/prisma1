package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object IntegrationTypeType {
  val Type = EnumType(
    "IntegrationTypeType",
    values = List(
      EnumValue("AUTH_PROVIDER", value = models.IntegrationType.AuthProvider),
      EnumValue("SEARCH_PROVIDER", value = models.IntegrationType.SearchProvider)
    )
  )
}
