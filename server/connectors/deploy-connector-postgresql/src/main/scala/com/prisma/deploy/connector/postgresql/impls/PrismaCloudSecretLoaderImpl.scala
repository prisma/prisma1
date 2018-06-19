package com.prisma.deploy.connector.postgresql.impls

import com.prisma.metrics.PrismaCloudSecretLoader

import scala.concurrent.Future

case class PrismaCloudSecretLoaderImpl() extends PrismaCloudSecretLoader {
  override def loadCloudSecret() = Future.successful(None)
}
