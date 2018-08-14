package com.prisma.deploy.connector.mongo.impls.mutactions

import com.prisma.deploy.connector.mongo.database.MongoDeployDatabaseMutationBuilder
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}

object CreateModelInterpreter extends MongoMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable) = {
    MongoDeployDatabaseMutationBuilder.createTable(projectId = mutaction.projectId, name = mutaction.model.dbName)
  }

  override def rollback(mutaction: CreateModelTable) = {
    MongoDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model.dbName)
  }
}

object DeleteModelInterpreter extends MongoMutactionInterpreter[DeleteModelTable] {
  // TODO: this is not symmetric

  override def execute(mutaction: DeleteModelTable) = {
    val dropTable = MongoDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model.dbName)
    val dropScalarListFields =
      mutaction.scalarListFields.map(field => MongoDeployDatabaseMutationBuilder.dropScalarListTable(mutaction.projectId, mutaction.model.dbName, field))

    dropTable
  }

  override def rollback(mutaction: DeleteModelTable) = {
    MongoDeployDatabaseMutationBuilder.createTable(projectId = mutaction.projectId, name = mutaction.model.dbName)
  }
}

object RenameModelInterpreter extends MongoMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String) = {
    val changeModelTableName = MongoDeployDatabaseMutationBuilder.renameTable(projectId = mutaction.projectId, name = previousName, newName = nextName)
    val changeScalarListFieldTableNames = mutaction.scalarListFieldsNames.map { fieldName =>
      MongoDeployDatabaseMutationBuilder.renameScalarListTable(mutaction.projectId, previousName, fieldName, nextName, fieldName)
    }

    changeModelTableName
  }
}
