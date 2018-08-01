package com.prisma.deploy.schema.types

import com.prisma.config.PrismaConfig
import com.prisma.deploy.schema.SystemUserContext
import sangria.schema._

object ServerInfoType {
  val version = sys.env.getOrElse("CLUSTER_VERSION", sys.error("Env var CLUSTER_VERSION required but not found."))
  val commit  = sys.env.getOrElse("COMMIT_SHA", sys.error("Env var COMMIT_SHA required but not found."))

  def Type(config: PrismaConfig): ObjectType[SystemUserContext, Unit] = ObjectType(
    "ServerInfo",
    "Information about the deployed server",
    fields[SystemUserContext, Unit](
      Field("version", StringType, resolve = _ => version),
      Field("commit", StringType, resolve = _ => commit),
      Field("primaryConnector", StringType, resolve = _ => config.databases.headOption.map(_.connector).getOrElse("no_connector"))
    )
  )
}
