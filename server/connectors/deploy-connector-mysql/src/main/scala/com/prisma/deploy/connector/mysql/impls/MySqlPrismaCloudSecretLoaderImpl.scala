package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.CloudSecretPersistence

case class MySqlPrismaCloudSecretLoaderImpl() extends CloudSecretPersistence {
  override def load() = ???

  override def update(secret: Option[String]) = ???
}
