package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema._

object FeatureToggle {
  lazy val Type: ObjectType[SystemUserContext, models.FeatureToggle] = ObjectType(
    "FeatureToggle",
    "The feature toggles of a project.",
    interfaces[SystemUserContext, models.FeatureToggle](nodeInterface),
    idField[SystemUserContext, models.FeatureToggle] ::
      fields[SystemUserContext, models.FeatureToggle](
      Field("name", StringType, resolve = _.value.name),
      Field("isEnabled", BooleanType, resolve = _.value.isEnabled)
    )
  )
}
