package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.CloudSecretPersistence
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class CloudSecretPersistenceImpl(internalDatabase: MongoDatabase)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  val secrets = internalDatabase.getCollection("Secret")

  override def load() = {
    val first: Future[Option[Document]] = secrets.find().collect().toFuture().map(_.headOption)
    first.map { docOption =>
      docOption.map(doc => doc.get[BsonString]("secret").get.toString)
    }
  }

  override def update(secret: Option[String]) = {
    secrets.updateMany(Filters.and(), Document("secret" -> secret)).toFuture()
  }
}
