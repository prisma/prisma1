package com.prisma.image

import com.prisma.metrics.PrismaCloudSecretLoader

import scala.concurrent.Future

class PrismaCloudSecretLoaderImpl extends PrismaCloudSecretLoader {
  override def loadCloudSecret() = Future.successful(None)
}
