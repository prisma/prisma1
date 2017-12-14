package cool.graph.deploy.schema.types

import build_info.BuildInfo
import cool.graph.deploy.schema.SystemUserContext
import sangria.schema._

object ClusterInfoType {
  lazy val Type: ObjectType[SystemUserContext, Unit] = ObjectType(
    "ClusterInfo",
    "Information about the deployed cluster",
    fields[SystemUserContext, Unit](
      Field("version", StringType, resolve = _ => BuildInfo.imageTag)
    )
  )
}
