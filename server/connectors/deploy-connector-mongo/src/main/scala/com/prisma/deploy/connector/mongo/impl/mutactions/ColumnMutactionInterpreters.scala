package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.{CreateColumn, DeleteColumn, UpdateColumn}
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object CreateColumnInterpreter extends MongoMutactionInterpreter[CreateColumn] {
  override def execute(mutaction: CreateColumn) = mutaction.field.isUnique && !mutaction.field.isId match {
    case true  => MongoDeployDatabaseMutationBuilder.createIndex(mutaction.model, mutaction.field.dbName)
    case false => NoAction.unit
  }

  override def rollback(mutaction: CreateColumn) = mutaction.field.isUnique && !mutaction.field.isId match {
    case true  => MongoDeployDatabaseMutationBuilder.deleteIndex(mutaction.model, mutaction.field.dbName)
    case false => NoAction.unit
  }
}

object DeleteColumnInterpreter extends MongoMutactionInterpreter[DeleteColumn] {
  override def execute(mutaction: DeleteColumn) = mutaction.field.isUnique && !mutaction.field.isId match {
    case true  => MongoDeployDatabaseMutationBuilder.deleteIndex(mutaction.model, mutaction.field.dbName)
    case false => NoAction.unit
  }

  override def rollback(mutaction: DeleteColumn) = mutaction.field.isUnique && !mutaction.field.isId match {
    case true  => MongoDeployDatabaseMutationBuilder.createIndex(mutaction.model, mutaction.field.dbName)
    case false => NoAction.unit
  }
}

object UpdateColumnInterpreter extends MongoMutactionInterpreter[UpdateColumn] {
  override def execute(mutaction: UpdateColumn) = (mutaction.oldField.isUnique, mutaction.newField.isUnique) match {
    case (false, true) => MongoDeployDatabaseMutationBuilder.createIndex(mutaction.model, mutaction.newField.dbName)
    case (true, false) => MongoDeployDatabaseMutationBuilder.deleteIndex(mutaction.model, mutaction.newField.dbName)
    case _             => NoAction.unit
  }

  override def rollback(mutaction: UpdateColumn) = (mutaction.oldField.isUnique, mutaction.newField.isUnique) match {
    case (false, true) => MongoDeployDatabaseMutationBuilder.deleteIndex(mutaction.model, mutaction.newField.dbName)
    case (true, false) => MongoDeployDatabaseMutationBuilder.createIndex(mutaction.model, mutaction.newField.dbName)
    case _             => NoAction.unit
  }
}
