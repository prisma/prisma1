package cool.graph.deploy.schema.types

import cool.graph.deploy.schema.SystemUserContext
import sangria.schema._

object ClusterInfoType {
  val version = sys.env.getOrElse("CLUSTER_VERSION", sys.error("Env var CLUSTER_VERSION required but not found."))

  lazy val Type: ObjectType[SystemUserContext, Unit] = ObjectType(
    "ClusterInfo",
    "Information about the deployed cluster",
    fields[SystemUserContext, Unit](
      Field("version", StringType, resolve = _ => version)
    )
  )
}
