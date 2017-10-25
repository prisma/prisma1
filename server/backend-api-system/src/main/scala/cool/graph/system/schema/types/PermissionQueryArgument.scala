package cool.graph.system.schema.types

import cool.graph.shared.models.TypeIdentifier
import cool.graph.system.SystemUserContext
import sangria.schema._

object PermissionQueryArgument {
  lazy val Type: ObjectType[SystemUserContext, PermissionQueryArguments.PermissionQueryArgument] =
    ObjectType(
      "PermissionQueryArgument",
      "PermissionQueryArgument",
      () =>
        fields[SystemUserContext, PermissionQueryArguments.PermissionQueryArgument](
          Field("name", StringType, resolve = _.value.name),
          Field("typeName", StringType, resolve = ctx => TypeIdentifier.toSangriaScalarType(ctx.value.typeIdentifier).name),
          Field("group", StringType, resolve = _.value.group)
      )
    )
}
