package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}

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

object RenameModelInterpreter extends MongoMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = NoAction.unit //setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = NoAction.unit //setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String) = {
    MongoDeployDatabaseMutationBuilder.renameCollection(projectId = mutaction.projectId, collectionName = previousName, newName = nextName)
  }
}
