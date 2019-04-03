package com.prisma.deploy.connector.mongo.database

import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

object MongoDeployDatabaseQueryBuilder {

  def existsByModel(clientDatabase: MongoClient, database: String, model: Model)(implicit ec: ExecutionContext): Future[Boolean] = {
    clientDatabase.getDatabase(database).getCollection(model.dbName).find().limit(1).toFuture().map(list => list.nonEmpty)
  }

  def existsByRelation(projectId: String, relationTableName: String) = ???

  def existsDuplicateByRelationAndSide(projectId: String, relationTableName: String, relationSide: RelationSide) = ???

  def existsDuplicateValueByModelAndField(projectId: String, modelName: String, fieldName: String) = ???

  def existsNullByModelAndScalarField(projectId: String, modelName: String, fieldName: String) = ???

  def existsNullByModelAndRelationField(projectId: String, modelName: String, field: RelationField) = ???

  def enumValueIsInUse(projectId: String, models: Vector[Model], enumName: String, value: String) = ???
}
