package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, DatabaseSchema, UpdateScalarListTable}
import slick.jdbc.PostgresProfile.api._

case class CreateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.createScalarListTable(
      projectId = mutaction.projectId,
      model = mutaction.model,
      fieldName = mutaction.field.dbName,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }

  override def rollback(mutaction: CreateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    DBIO.seq(
      builder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName, schemaBeforeMigration))
  }
}

case class DeleteScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    DBIO.seq(
      builder
        .dropScalarListTable(projectId = mutaction.projectId, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName, schemaBeforeMigration))
  }

  override def rollback(mutaction: DeleteScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.createScalarListTable(
      projectId = mutaction.projectId,
      model = mutaction.model,
      fieldName = mutaction.field.dbName,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }
}

case class UpdateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateScalarListTable] {
  override def execute(mutaction: UpdateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    val oldField  = mutaction.oldField
    val newField  = mutaction.newField
    val projectId = mutaction.projectId
    val oldModel  = mutaction.oldModel
    val newModel  = mutaction.newModel

    val updateType = if (oldField.typeIdentifier != newField.typeIdentifier) {
      builder.updateScalarListType(projectId, oldModel.dbName, oldField.dbName, newField.typeIdentifier)
    } else { DBIO.successful(()) }

    val renameTable = builder.renameTable(projectId, s"${oldModel.dbName}_${oldField.dbName}", s"${newModel.dbName}_${newField.dbName}")
    DBIO.seq(updateType, renameTable)
  }

  override def rollback(mutaction: UpdateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
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
