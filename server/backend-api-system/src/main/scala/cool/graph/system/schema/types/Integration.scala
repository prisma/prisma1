package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema._

object Integration {
  lazy val Type: InterfaceType[SystemUserContext, models.Integration] =
    InterfaceType(
      "Integration",
      "This is an integration. Use inline fragment to get values from the concrete type: `{id ... on SearchProviderAlgolia { algoliaSchema }}`",
      () =>
        fields[SystemUserContext, models.Integration](
          Field("id", IDType, resolve = _.value.id),
          Field("isEnabled", BooleanType, resolve = _.value.isEnabled),
          Field("name", IntegrationNameType.Type, resolve = _.value.name),
          Field("type", IntegrationTypeType.Type, resolve = _.value.integrationType)
      )
    )
}
