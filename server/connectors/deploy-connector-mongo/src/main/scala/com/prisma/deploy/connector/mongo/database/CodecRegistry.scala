package com.prisma.deploy.connector.mongo.database

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

object CodecRegistry {

  val codecRegistry = fromRegistries(fromProviders(classOf[Migration], classOf[ProjectDefinition]), DEFAULT_CODEC_REGISTRY)

}
