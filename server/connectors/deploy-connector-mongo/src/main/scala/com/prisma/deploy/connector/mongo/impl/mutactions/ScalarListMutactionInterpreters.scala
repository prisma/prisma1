package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.MongoDeployDatabaseMutationBuilder
import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, UpdateScalarListTable}

object CreateScalarListInterpreter extends MongoMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable) = {
    MongoDeployDatabaseMutationBuilder.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model.name,
      fieldName = mutaction.field.name,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }

  override def rollback(mutaction: CreateScalarListTable) = {
    MongoDeployDatabaseMutationBuilder
      .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.name, fieldName = mutaction.field.name)

  }
}

object DeleteScalarListInterpreter extends MongoMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable) = {
    MongoDeployDatabaseMutationBuilder
      .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.name, fieldName = mutaction.field.name)
  }

  override def rollback(mutaction: DeleteScalarListTable) = {
    MongoDeployDatabaseMutationBuilder.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model.name,
      fieldName = mutaction.field.name,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }
}

object UpdateScalarListInterpreter extends MongoMutactionInterpreter[UpdateScalarListTable] {
  override def execute(mutaction: UpdateScalarListTable) = {
    val oldField  = mutaction.oldField
    val newField  = mutaction.newField
    val projectId = mutaction.projectId
    val oldModel  = mutaction.oldModel
    val newModel  = mutaction.newModel

    val updateType = if (oldField.typeIdentifier != newField.typeIdentifier) {
      List(MongoDeployDatabaseMutationBuilder.updateScalarListType(projectId, oldModel.name, oldField.name, newField.typeIdentifier))
    } else {
      List.empty
    }

    val renameTable = if (oldField.name != newField.name || oldModel.name != newModel.name) {
      List(MongoDeployDatabaseMutationBuilder.renameScalarListTable(projectId, oldModel.name, oldField.name, newModel.name, newField.name))
    } else {
      List.empty
    }

    val changes = updateType ++ renameTable

//    if (changes.isEmpty) {
//      DBIO.successful(())
//    } else {
//      DBIO.seq(changes: _*)
//    }
    renameTable.head
  }

  override def rollback(mutaction: UpdateScalarListTable) = {
    val oppositeMutaction = UpdateScalarListTable(
      projectId = mutaction.projectId,
      oldModel = mutaction.newModel,
      newModel = mutaction.oldModel,
      oldField = mutaction.newField,
      newField = mutaction.oldField
    )
    execute(oppositeMutaction)
  }
}
