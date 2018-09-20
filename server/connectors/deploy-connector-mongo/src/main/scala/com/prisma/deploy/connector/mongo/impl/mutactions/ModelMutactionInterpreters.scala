package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}

object DeleteModelInterpreter extends MongoMutactionInterpreter[DeleteModelTable] {
  override def execute(mutaction: DeleteModelTable) = {
    MongoDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model.dbName)
  }

  override def rollback(mutaction: DeleteModelTable) = NoAction.unit
}

object RenameModelInterpreter extends MongoMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String) = {
    MongoDeployDatabaseMutationBuilder.renameTable(projectId = mutaction.projectId, name = previousName, newName = nextName)
  }
}
