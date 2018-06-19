package com.prisma.deploy.connector.mysql.impls

import com.prisma.metrics.PrismaCloudSecretLoader

import scala.concurrent.Future

case class MySqlPrismaCloudSecretLoaderImpl() extends PrismaCloudSecretLoader {
  override def loadCloudSecret() = Future.successful(None)
}
