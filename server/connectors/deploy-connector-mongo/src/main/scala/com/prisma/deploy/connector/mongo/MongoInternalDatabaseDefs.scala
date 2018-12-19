package com.prisma.deploy.connector.mongo

import com.mongodb.ConnectionString
import com.prisma.config.DatabaseConfig
import org.mongodb.scala.MongoClient

case class MongoInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  val uri: String = dbConfig.uri
  if (new ConnectionString(uri).getSslEnabled) System.setProperty("org.mongodb.async.type", "netty")
  val client = MongoClient(uri)
}
