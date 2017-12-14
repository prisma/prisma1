package cool.graph.deploy.schema.types

import cool.graph.deploy.schema.SystemUserContext
import cool.graph.shared.models
import sangria.schema._

object ClusterInfoType {
  lazy val Type: ObjectType[SystemUserContext, Unit] = ObjectType(
    "ClusterInfo",
    "Information about the deployed cluster",
    fields[SystemUserContext, Unit](
      Field("version", StringType, resolve = _ => "1.0-beta1")
    )
  )
}
