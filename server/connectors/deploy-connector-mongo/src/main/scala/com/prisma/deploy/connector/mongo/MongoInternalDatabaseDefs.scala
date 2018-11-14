package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala.MongoClient

case class MongoInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  val uri: String = (dbConfig.database, dbConfig.ssl) match {
    case (None, false)    => s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=admin"
    case (None, true)     => s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=admin&ssl=true"
    case (Some(db), true) => s"mongodb+srv://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}/$db"
    case (_, _)           => sys.error("Database provided, but ssl set to true.")
  }

  System.setProperty("org.mongodb.async.type", "netty")

  val client = MongoClient(uri)
}
