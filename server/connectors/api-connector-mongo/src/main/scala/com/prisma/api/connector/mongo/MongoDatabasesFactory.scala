package com.prisma.api.connector.mongo

import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.prisma.config.DatabaseConfig
import org.mongodb.scala._
object MongoDatabasesFactory {

  def initialize(config: DatabaseConfig) = {
//    val uri: String = (config.database, config.ssl) match {
//      case (None, false)    => s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=admin"
//      case (None, true)     => s"mongodb://${config.user}:${config.password.getOrElse("")}@${config.host}:${config.port}/?authSource=admin&ssl=true"
//      case (Some(db), true) => s"mongodb+srv://${config.user}:${config.password.getOrElse("")}@${config.host}/$db"
//      case (_, _)           => sys.error("Database provided, but ssl set to true.")
//    }
//
//    System.setProperty("org.mongodb.async.type", "netty")
//
//    println(s"mongoUri: $uri")
//
//    MongoClient(uri)

    import com.mongodb.MongoCredential._

    import collection.JavaConverters._

    // ...

    val user: String          = config.user // the user name
    val source: String        = config.database.getOrElse("admin") // the source where the user is defined
    val password: Array[Char] = config.password.getOrElse("").toCharArray // the password as a character array
    // ...
    val credential: MongoCredential = createCredential(user, source, password)

    val settings: MongoClientSettings = config.ssl match {
      case true =>
        MongoClientSettings
          .builder()
          .applyToClusterSettings(b => b.hosts(List(new ServerAddress(config.host, config.port)).asJava))
          .credential(credential)
          .applyToSslSettings(b => b.enabled(true).build())
          .streamFactoryFactory(NettyStreamFactoryFactory.builder().build())
          .build()

      case false =>
        MongoClientSettings
          .builder()
          .applyToClusterSettings(b => b.hosts(List(new ServerAddress(config.host, config.port)).asJava))
          .credential(credential)
          .applyToSslSettings(b => b.enabled(false).build())
          .build()
    }

    MongoClient(settings)

  }
}
