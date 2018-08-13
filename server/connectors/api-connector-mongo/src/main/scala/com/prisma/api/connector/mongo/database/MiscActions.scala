package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.{MutactionResults, ResetData, UnitDatabaseMutactionResult}
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

trait MiscActions {

  def truncateTables(mutaction: ResetData)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val project       = mutaction.project
    val modelNames    = project.models.map(_.name)
    val relationNames = project.relations.map(_.name)

    val actions = (relationNames ++ modelNames).map { name =>
      val collection: MongoCollection[Document] = database.getCollection(name)
      collection.drop().toFuture()
    }

    Future.sequence(actions).map(_ => MutactionResults(Vector(UnitDatabaseMutactionResult)))
  }
}
