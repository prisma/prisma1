package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala.MongoClient

case class MongoInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  val authSource = dbConfig.authSource.getOrElse("admin")

  val uri: String = dbConfig.protocol match {
    case Some("mongodb") =>
      s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=$authSource&ssl=${dbConfig.ssl}"

    case Some("mongodb+srv") =>
      s"mongodb+srv://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}/$authSource?ssl=${dbConfig.ssl}"

    case _ =>
      sys.error("Invalid Mongo protocol specified")
  }

  if (dbConfig.ssl) System.setProperty("org.mongodb.async.type", "netty")

  println(s"mongoUri: $uri")

  val client = MongoClient(uri)

//  import com.mongodb.MongoCredential._
//  import collection.JavaConverters._
//
//  // ...
//
//  val user: String          = dbdbConfig.user // the user name
//  val source: String        = dbdbConfig.database.getOrElse("admin") // the source where the user is defined
//  val password: Array[Char] = dbdbConfig.password.getOrElse("").toCharArray // the password as a character array
//  // ...
//  val credential: MongoCredential = createCredential(user, source, password)
//
//  val settings: MongoClientSettings = dbdbConfig.ssl match {
//    case true =>
//      System.setProperty("org.mongodb.async.type", "netty")
//
//      MongoClientSettings
//        .builder()
//        .applyToClusterSettings(b => b.hosts(List(new ServerAddress(dbdbConfig.host, dbdbConfig.port)).asJava))
//        .credential(credential)
//        .applyToSslSettings(b => b.enabled(true).build())
//        .build()
//
//    case false =>
//      MongoClientSettings
//        .builder()
//        .applyToClusterSettings(b => b.hosts(List(new ServerAddress(dbdbConfig.host, dbdbConfig.port)).asJava))
//        .credential(credential)
//        .applyToSslSettings(b => b.enabled(false).build())
//        .build()
//  }
//
//  val client = MongoClient(settings)

}
