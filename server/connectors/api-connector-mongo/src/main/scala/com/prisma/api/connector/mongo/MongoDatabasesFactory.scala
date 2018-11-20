package com.prisma.api.connector.mongo

import com.mongodb.ConnectionString
import com.prisma.config.DatabaseConfig
import org.mongodb.scala._
object MongoDatabasesFactory {

  def initialize(config: DatabaseConfig) = {
    val uri: String = config.uri
    if (new ConnectionString(uri).getSslEnabled) System.setProperty("org.mongodb.async.type", "netty")
    MongoClient(uri)
  }
}
