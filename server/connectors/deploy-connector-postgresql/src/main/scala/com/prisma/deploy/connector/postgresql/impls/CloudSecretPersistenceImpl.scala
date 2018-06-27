package com.prisma.deploy.connector.postgresql.impls

import com.prisma.deploy.connector.CloudSecretPersistence
import com.prisma.deploy.connector.postgresql.database.CloudSecretTable
import slick.jdbc.PostgresProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext

case class CloudSecretPersistenceImpl(internalDatabase: DatabaseDef)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  override def load() = internalDatabase.run(CloudSecretTable.getSecret)

  override def update(secret: Option[String]) = internalDatabase.run(CloudSecretTable.setSecret(secret))
}
