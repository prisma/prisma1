package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector.mysql.database.MySqlDeployDatabaseMutationBuilder
import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, UpdateScalarListTable}
import slick.jdbc.MySQLProfile.api._

object CreateScalarListInterpreter extends SqlMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable) = {
    MySqlDeployDatabaseMutationBuilder.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model.name,
      fieldName = mutaction.field.name,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }

  override def rollback(mutaction: CreateScalarListTable) = {
    DBIO.seq(
      MySqlDeployDatabaseMutationBuilder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.name, fieldName = mutaction.field.name))
  }
}

object DeleteScalarListInterpreter extends SqlMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable) = {
    DBIO.seq(
      MySqlDeployDatabaseMutationBuilder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.name, fieldName = mutaction.field.name))
  }

  override def rollback(mutaction: DeleteScalarListTable) = {
    MySqlDeployDatabaseMutationBuilder.createScalarListTable(
      projectId = mutaction.projectId,
      modelName = mutaction.model.name,
      fieldName = mutaction.field.name,
      typeIdentifier = mutaction.field.typeIdentifier
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
      List(MySqlDeployDatabaseMutationBuilder.updateScalarListType(projectId, oldModel.name, oldField.name, newField.typeIdentifier))
    } else {
      List.empty
    }

    val renameTable = if (oldField.name != newField.name || oldModel.name != newModel.name) {
      List(MySqlDeployDatabaseMutationBuilder.renameScalarListTable(projectId, oldModel.name, oldField.name, newModel.name, newField.name))
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
