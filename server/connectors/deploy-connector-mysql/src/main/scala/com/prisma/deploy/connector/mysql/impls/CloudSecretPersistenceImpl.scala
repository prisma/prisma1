package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.CloudSecretPersistence
import com.prisma.deploy.connector.mysql.database.CloudSecretTable

import scala.concurrent.ExecutionContext

import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class CloudSecretPersistenceImpl(internalDatabase: DatabaseDef)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  override def load() = internalDatabase.run(CloudSecretTable.getSecret)

  override def update(secret: Option[String]) = internalDatabase.run(CloudSecretTable.setSecret(secret))
}
