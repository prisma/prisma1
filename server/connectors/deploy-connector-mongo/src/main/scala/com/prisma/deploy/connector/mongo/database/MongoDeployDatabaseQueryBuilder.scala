package com.prisma.deploy.connector.mongo.database

import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Model, RelationField}

object MongoDeployDatabaseQueryBuilder {

  def existsByModel(projectId: String, modelName: String) = ???

  def existsByRelation(projectId: String, relationTableName: String) = ???

  def existsDuplicateByRelationAndSide(projectId: String, relationTableName: String, relationSide: RelationSide) = ???

  def existsDuplicateValueByModelAndField(projectId: String, modelName: String, fieldName: String) = ???

  def existsNullByModelAndScalarField(projectId: String, modelName: String, fieldName: String) = ???

  def existsNullByModelAndRelationField(projectId: String, modelName: String, field: RelationField) = ???

  def enumValueIsInUse(projectId: String, models: Vector[Model], enumName: String, value: String) = ???
}
