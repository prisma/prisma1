package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}

object CreateModelInterpreter extends MongoMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable) = {
    MongoDeployDatabaseMutationBuilder.createCollection(collectionName = mutaction.model.dbName)
  }

  override def rollback(mutaction: CreateModelTable) = {
    MongoDeployDatabaseMutationBuilder.dropCollection(collectionName = mutaction.model.dbName)
  }
}

object DeleteModelInterpreter extends MongoMutactionInterpreter[DeleteModelTable] {
  override def execute(mutaction: DeleteModelTable) = {
    MongoDeployDatabaseMutationBuilder.dropCollection(collectionName = mutaction.model.dbName)
  }

  override def rollback(mutaction: DeleteModelTable) = {
    MongoDeployDatabaseMutationBuilder.createCollection(collectionName = mutaction.model.dbName)
  }
}

object RenameModelInterpreter extends MongoMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String) = {
    MongoDeployDatabaseMutationBuilder.renameCollection(projectId = mutaction.projectId, collectionName = previousName, newName = nextName)
  }
}
