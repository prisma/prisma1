package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector.mysql.database.DeployDatabaseMutationBuilderMySql
import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, UpdateScalarListTable}
import slick.jdbc.MySQLProfile.api._

object CreateScalarListInterpreter extends SqlMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable) = {
    DeployDatabaseMutationBuilderMySql.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model,
      fieldName = mutaction.field,
      typeIdentifier = mutaction.typeIdentifier
    )
  }

  override def rollback(mutaction: CreateScalarListTable) = {
    DBIO.seq(DeployDatabaseMutationBuilderMySql.dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model, fieldName = mutaction.field))
  }
}

object DeleteScalarListInterpreter extends SqlMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable) = {
    DBIO.seq(DeployDatabaseMutationBuilderMySql.dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model, fieldName = mutaction.field))
  }

  override def rollback(mutaction: DeleteScalarListTable) = {
    DeployDatabaseMutationBuilderMySql.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model,
      fieldName = mutaction.field,
      typeIdentifier = mutaction.typeIdentifier
    )
  }
}

object UpdateScalarListInterpreter extends SqlMutactionInterpreter[UpdateScalarListTable] {
  override def execute(mutaction: UpdateScalarListTable) = {
    val oldField  = mutaction.oldField
    val newField  = mutaction.newField
    val projectId = mutaction.projectId
    val oldModel  = mutaction.oldModel
    val newModel  = mutaction.newModel

    val updateType = if (oldField.typeIdentifier != newField.typeIdentifier) {
      List(DeployDatabaseMutationBuilderMySql.updateScalarListType(projectId, oldModel.name, oldField.name, newField.typeIdentifier))
    } else {
      List.empty
    }

    val renameTable = if (oldField.name != newField.name || oldModel.name != newModel.name) {
      List(DeployDatabaseMutationBuilderMySql.renameScalarListTable(projectId, oldModel.name, oldField.name, newModel.name, newField.name))
    } else {
      List.empty
    }

    val changes = updateType ++ renameTable

    if (changes.isEmpty) {
      DBIO.successful(())
    } else {
      DBIO.seq(changes: _*)
    }
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
