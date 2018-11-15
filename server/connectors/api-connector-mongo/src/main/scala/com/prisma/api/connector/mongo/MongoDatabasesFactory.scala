package com.prisma.api.connector.mongo

import com.prisma.config.DatabaseConfig
import org.mongodb.scala._
object MongoDatabasesFactory {

  def initialize(config: DatabaseConfig) = {
    val authSource = config.authSource.getOrElse("admin")

    val uri: String = config.protocol match {
      case Some("mongodb") =>
        s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=$authSource&ssl=${config.ssl}"

      case Some("mongodb+srv") =>
        s"mongodb+srv://${config.user}:${config.password.getOrElse("")}@${config.host}/$authSource?ssl=${config.ssl}"

      case _ =>
        sys.error("Invalid Mongo protocol specified")
    }

    if (config.ssl) System.setProperty("org.mongodb.async.type", "netty")

    println(s"mongoUri: $uri")

    MongoClient(uri)

//    import com.mongodb.MongoCredential._
//
//    import collection.JavaConverters._
//
//    // ...
//
//    val user: String          = config.user // the user name
//    val source: String        = config.database.getOrElse("admin") // the source where the user is defined
//    val password: Array[Char] = config.password.getOrElse("").toCharArray // the password as a character array
//    // ...
//    val credential: MongoCredential = createCredential(user, source, password)
//
//    val settings: MongoClientSettings = config.ssl match {
//      case true =>
//
//        MongoClientSettings
//          .builder()
//          .applyToClusterSettings(b => b.hosts(List(new ServerAddress(config.host, config.port)).asJava))
//          .credential(credential)
//          .applyToSslSettings(b => b.enabled(true).build())
//          .streamFactoryFactory(NettyStreamFactoryFactory.builder().build())
//          .build()
//
//      case false =>
//        MongoClientSettings
//          .builder()
//          .applyToClusterSettings(b => b.hosts(List(new ServerAddress(config.host, config.port)).asJava))
//          .credential(credential)
//          .applyToSslSettings(b => b.enabled(false).build())
//          .build()
//    }
//
//    MongoClient(settings)

  }
}
