package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.{MutactionResults, ResetData, UnitDatabaseMutactionResult}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Sorts.ascending

import scala.concurrent.{ExecutionContext, Future}

trait MiscActions {

  def truncateTables(mutaction: ResetData)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val project           = mutaction.project
    val nonEmbeddedModels = project.models.filter(!_.isEmbedded)

    for {
      _ <- Future.sequence(nonEmbeddedModels.map(model => database.getCollection(model.dbName).drop().toFuture()))
      _ <- Future.sequence(nonEmbeddedModels.map { model =>
            val fieldsWithUniqueConstraint = model.scalarNonListFields.filter(_.isUnique)
            Future.sequence(database.createCollection(model.dbName).toFuture() +: fieldsWithUniqueConstraint.map(field =>
              addUniqueConstraint(database, model.dbName, field.name)))
          })
      _ <- Future.sequence(project.relations.map { relation =>
            if (relation.isInlineRelation) {
              relation.modelAField.relationIsInlinedInParent match {
                case true  => addRelationIndex(database, relation.modelAField.model.dbName, relation.modelAField.dbName)
                case false => addRelationIndex(database, relation.modelBField.model.dbName, relation.modelBField.dbName)
              }
            } else {
              Future.successful(())
            }
          })
    } yield MutactionResults(Vector(UnitDatabaseMutactionResult))
  }

  def addUniqueConstraint(database: MongoDatabase, collectionName: String, fieldName: String)(implicit ec: ExecutionContext) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, true)

    database
      .getCollection(collectionName)
      .createIndex(ascending(fieldName), IndexOptions().unique(true).sparse(true).name(shortenedName))
      .toFuture()
      .map(_ => ())
  }

  def addRelationIndex(database: MongoDatabase, collectionName: String, fieldName: String)(implicit ec: ExecutionContext) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, false)

    database
      .getCollection(collectionName)
      .createIndex(ascending(fieldName), IndexOptions().name(shortenedName))
      .toFuture()
      .map(_ => ())
  }

  def indexNameHelper(collectionName: String, fieldName: String, unique: Boolean): String = {
    val shortenedName = fieldName.substring(0, (125 - 25 - collectionName.length - 12).min(fieldName.length))

    unique match {
      case false => shortenedName + "_R"
      case true  => shortenedName + "_U"
    }
  }
}
