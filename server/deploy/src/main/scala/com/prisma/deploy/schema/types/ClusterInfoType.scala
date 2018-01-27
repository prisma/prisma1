package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import sangria.schema._

object ClusterInfoType {
  val version = sys.env.getOrElse("CLUSTER_VERSION", sys.error("Env var CLUSTER_VERSION required but not found."))
  val commit  = sys.env.getOrElse("COMMIT_SHA", sys.error("Env var COMMIT_SHA required but not found."))

  lazy val Type: ObjectType[SystemUserContext, Unit] = ObjectType(
    "ClusterInfo",
    "Information about the deployed cluster",
    fields[SystemUserContext, Unit](
      Field("version", StringType, resolve = _ => version),
      Field("commit", StringType, resolve = _ => commit)
    )
  )
}
