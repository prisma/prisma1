package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala.MongoClient

case class MongoInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  val uri: String = s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=admin"
  val client      = MongoClient(uri)
}
