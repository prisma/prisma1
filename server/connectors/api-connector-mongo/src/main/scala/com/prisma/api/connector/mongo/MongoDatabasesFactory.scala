package com.prisma.api.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala._
object MongoDatabasesFactory {

  def initialize(config: DatabaseConfig) = {
    val uri: String = (config.database, config.ssl) match {
      case (None, false)    => s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=admin"
      case (None, true)     => s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=admin&ssl=true"
      case (Some(db), true) => s"mongodb+srv://${config.user}:${config.password.getOrElse("")}@${config.host}/$db"
      case (_, _)           => sys.error("Database provided, but ssl set to true.")
    }

    System.setProperty("org.mongodb.async.type", "netty")

    println(s"mongoUri: $uri")

    MongoClient(uri)
  }
}
