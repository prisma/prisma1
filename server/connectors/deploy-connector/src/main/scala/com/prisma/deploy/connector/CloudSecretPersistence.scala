package com.prisma.deploy.connector

import com.prisma.metrics.PrismaCloudSecretLoader

import scala.concurrent.Future

trait CloudSecretPersistence extends PrismaCloudSecretLoader {
  def load(): Future[Option[String]]
  def update(secret: Option[String]): Future[_]

  // implement inherited interface
  override def loadCloudSecret(): Future[Option[String]] = load()
}
