package com.prisma.api.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala._
object MongoDatabasesFactory {

  def initialize(config: DatabaseConfig) = {
    val uri: String = s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=admin"
    println(s"mongoUri: $uri")

    // directly connect to the default server localhost on port 27017
    val mongoClient: MongoClient = MongoClient(uri)

    val database: MongoDatabase = mongoClient.getDatabase("test")
    Thread.sleep(5000)

    database
  }
}
