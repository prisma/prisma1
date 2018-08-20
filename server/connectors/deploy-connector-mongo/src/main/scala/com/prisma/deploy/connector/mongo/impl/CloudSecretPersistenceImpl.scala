package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.CloudSecretPersistence
import org.mongodb.scala.MongoClient

import scala.concurrent.ExecutionContext

case class CloudSecretPersistenceImpl(internalDatabase: MongoClient)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  override def load() = {
//    internalDatabase.run(CloudSecretTable.getSecret)
    ???
  }
  override def update(secret: Option[String]) = {
//    internalDatabase.run(CloudSecretTable.setSecret(secret))
    ???
  }
}
