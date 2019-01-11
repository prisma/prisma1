package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.persistence.CloudSecretPersistence
import org.mongodb.scala.bson.{BsonNull, BsonString}
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.{Document, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class MongoCloudSecretPersistence(internalDatabase: MongoDatabase)(implicit ec: ExecutionContext) extends CloudSecretPersistence {
  val secrets = internalDatabase.getCollection("Secret")

  override def load(): Future[Option[String]] = {
    secrets.find().toFuture().map { documents =>
      for {
        first  <- documents.headOption
        secret <- Option(first.getString("secret"))
      } yield secret
    }
  }

  override def update(secret: Option[String]): Future[_] = {
    val bsonValue = secret match {
      case Some(x) => BsonString(x)
      case None    => BsonNull()
    }
    secrets
      .replaceOne(
        filter = Document.empty,
        replacement = Document("secret" -> bsonValue),
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
  }
}
