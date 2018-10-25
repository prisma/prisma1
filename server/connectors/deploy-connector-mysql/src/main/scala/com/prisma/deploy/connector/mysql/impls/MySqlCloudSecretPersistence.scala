package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.mysql.database.CloudSecretTable
import com.prisma.deploy.connector.persistence.CloudSecretPersistence
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

case class MySqlCloudSecretPersistence(internalDatabase: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  override def load() = internalDatabase.run(CloudSecretTable.getSecret)

  override def update(secret: Option[String]) = internalDatabase.run(CloudSecretTable.setSecret(secret))
}
