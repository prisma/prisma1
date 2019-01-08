package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, Tables, UpdateScalarListTable}
import slick.jdbc.PostgresProfile.api._

case class CreateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable, schemaBeforeMigration: Tables) = {
    builder.createScalarListTable(
      projectId = mutaction.projectId,
      model = mutaction.model,
      fieldName = mutaction.field.dbName,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }

  override def rollback(mutaction: CreateScalarListTable, schemaBeforeMigration: Tables) = {
    DBIO.seq(
      builder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName))
  }
}

case class DeleteScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable, schemaBeforeMigration: Tables) = {
    DBIO.seq(
      builder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName))
  }

  override def rollback(mutaction: DeleteScalarListTable, schemaBeforeMigration: Tables) = {
    builder.createScalarListTable(
      projectId = mutaction.projectId,
      model = mutaction.model,
      fieldName = mutaction.field.dbName,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }
}

case class UpdateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateScalarListTable] {
  override def execute(mutaction: UpdateScalarListTable, schemaBeforeMigration: Tables) = {
    val oldField  = mutaction.oldField
    val newField  = mutaction.newField
    val projectId = mutaction.projectId
    val oldModel  = mutaction.oldModel
    val newModel  = mutaction.newModel

    val updateType = if (oldField.typeIdentifier != newField.typeIdentifier) {
      List(builder.updateScalarListType(projectId, oldModel.dbName, oldField.dbName, newField.typeIdentifier))
    } else {
      List.empty
    }

    val renameTable = if (oldField.dbName != newField.dbName || oldModel.dbName != newModel.dbName) {
      List(builder.renameScalarListTable(projectId, oldModel.dbName, oldField.dbName, newModel.dbName, newField.dbName))
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

  override def rollback(mutaction: UpdateScalarListTable, schemaBeforeMigration: Tables) = {
    val oppositeMutaction = UpdateScalarListTable(
      projectId = mutaction.projectId,
      oldModel = mutaction.newModel,
      newModel = mutaction.oldModel,
      oldField = mutaction.newField,
      newField = mutaction.oldField
    )
    execute(oppositeMutaction, schemaBeforeMigration)
  }
}
