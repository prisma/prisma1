package com.prisma.deploy.connector.mongo

import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.prisma.config.DatabaseConfig
import org.mongodb.scala.connection.{ClusterSettings, SslSettings}
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoCredential, ServerAddress}

case class MongoInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  val uri: String = (dbConfig.database, dbConfig.ssl) match {
    case (None, false) =>
      s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=admin"
    case (None, true) =>
      System.setProperty("org.mongodb.async.type", "netty")

      s"mongodb://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}:${dbConfig.port}/?authSource=admin&ssl=true"
    case (Some(db), true) =>
      System.setProperty("org.mongodb.async.type", "netty")

      s"mongodb+srv://${dbConfig.user}:${dbConfig.password.getOrElse("")}@${dbConfig.host}/$db"
    case (_, _) => sys.error("Database provided, but ssl set to true.")
  }

  println(s"mongoUri: $uri")

  val client = MongoClient(uri)

//  import com.mongodb.MongoCredential._
//  import collection.JavaConverters._
//
//  // ...
//
//  val user: String          = dbConfig.user // the user name
//  val source: String        = dbConfig.database.getOrElse("admin") // the source where the user is defined
//  val password: Array[Char] = dbConfig.password.getOrElse("").toCharArray // the password as a character array
//  // ...
//  val credential: MongoCredential = createCredential(user, source, password)
//
//  val settings: MongoClientSettings = dbConfig.ssl match {
//    case true =>
//      System.setProperty("org.mongodb.async.type", "netty")
//
//      MongoClientSettings
//        .builder()
//        .applyToClusterSettings(b => b.hosts(List(new ServerAddress(dbConfig.host, dbConfig.port)).asJava))
//        .credential(credential)
//        .applyToSslSettings(b => b.enabled(true).build())
//        .build()
//
//    case false =>
//      MongoClientSettings
//        .builder()
//        .applyToClusterSettings(b => b.hosts(List(new ServerAddress(dbConfig.host, dbConfig.port)).asJava))
//        .credential(credential)
//        .applyToSslSettings(b => b.enabled(false).build())
//        .build()
//  }
//
//  val client = MongoClient(settings)

}
