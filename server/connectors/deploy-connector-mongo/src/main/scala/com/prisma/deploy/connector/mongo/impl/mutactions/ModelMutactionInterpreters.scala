package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, UpdateModelTable}

object CreateModelInterpreter extends MongoMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable) = mutaction.model.isEmbedded match {
    case true  => NoAction.unit
    case false => MongoDeployDatabaseMutationBuilder.createCollection(collectionName = mutaction.model.dbName)
  }

  override def rollback(mutaction: CreateModelTable) = mutaction.model.isEmbedded match {
    case true  => NoAction.unit
    case false => MongoDeployDatabaseMutationBuilder.dropCollection(collectionName = mutaction.model.dbName)
  }
}

object DeleteModelInterpreter extends MongoMutactionInterpreter[DeleteModelTable] {
  override def execute(mutaction: DeleteModelTable) = mutaction.model.isEmbedded match {
    case true  => NoAction.unit
    case false => NoAction.unit //MongoDeployDatabaseMutationBuilder.dropCollection(collectionName = mutaction.model.dbName)
  }

  override def rollback(mutaction: DeleteModelTable) = mutaction.model.isEmbedded match {
    case true  => NoAction.unit
    case false => NoAction.unit //MongoDeployDatabaseMutationBuilder.createCollection(collectionName = mutaction.model.dbName)
  }
}

object UpdateModelInterpreter extends MongoMutactionInterpreter[UpdateModelTable] {
  override def execute(mutaction: UpdateModelTable)  = NoAction.unit //setName(mutaction, mutaction.previousName, mutaction.nextName)
  override def rollback(mutaction: UpdateModelTable) = NoAction.unit //setName(mutaction, mutaction.nextName, mutaction.previousName)
//
//  private def setName(mutaction: UpdateModelTable, previousName: String, nextName: String) = {
//    MongoDeployDatabaseMutationBuilder.renameCollection(mutaction.project, collectionName = previousName, newName = nextName)
//  }
}
