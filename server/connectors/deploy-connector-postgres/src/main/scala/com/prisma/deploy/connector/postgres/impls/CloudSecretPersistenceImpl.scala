package com.prisma.deploy.connector.postgres.impls

import com.prisma.deploy.connector.persistence.CloudSecretPersistence
import com.prisma.deploy.connector.postgres.database.CloudSecretTable
import slick.jdbc.PostgresProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext

case class CloudSecretPersistenceImpl(internalDatabase: DatabaseDef)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  override def load() = internalDatabase.run(CloudSecretTable.getSecret)

  override def update(secret: Option[String]) = internalDatabase.run(CloudSecretTable.setSecret(secret))
}
