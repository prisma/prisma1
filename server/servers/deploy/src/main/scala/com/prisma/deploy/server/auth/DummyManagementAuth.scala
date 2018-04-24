package com.prisma.deploy.server.auth

import scala.util.{Success, Try}

case class DummyManagementAuth() extends ManagementAuth {
  override def verify(name: String, stage: String, authHeaderOpt: Option[String]): Try[Unit] = {
    println(
      "Warning: Cluster authentication is disabled. " +
        "To protect your cluster you should provide one (not both) of the environment variables " +
        "'CLUSTER_PUBLIC_KEY' (asymmetric, deprecated soon) or 'PRISMA_MANAGEMENT_API_JWT_SECRET' (symmetric)."
    )

    Success(())
  }
}
