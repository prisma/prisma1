package com.prisma.api.connector.mongo

import org.mongodb.scala._
object MongoDatabasesFactory {
  val uri: String = "mongodb://prisma:prisma@localhost:27017/?authSource=admin"

  // directly connect to the default server localhost on port 27017
  val mongoClient: MongoClient = MongoClient(uri)

  val database: MongoDatabase = mongoClient.getDatabase("test")
}
