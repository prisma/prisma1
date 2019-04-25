package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{CreateScalarListTable, DeleteScalarListTable, DatabaseSchema, UpdateScalarListTable}
import slick.jdbc.PostgresProfile.api._

case class CreateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateScalarListTable] {
  override def execute(mutaction: CreateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table(s"${mutaction.model.dbName}_${mutaction.field.dbName}") match {
      case None =>
        builder.createScalarListTable(
          mutaction.project,
          model = mutaction.model,
          fieldName = mutaction.field.dbName,
          typeIdentifier = mutaction.field.typeIdentifier
        )

      case Some(_) =>
        DBIO.successful(())
    }
  }

  override def rollback(mutaction: CreateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    DBIO.seq(
      builder
        .dropScalarListTable(mutaction.project, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName, schemaBeforeMigration))
  }
}

case class DeleteScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteScalarListTable] {
  override def execute(mutaction: DeleteScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    DBIO.seq(
      builder
        .dropScalarListTable(mutaction.project, modelName = mutaction.model.dbName, fieldName = mutaction.field.dbName, schemaBeforeMigration))
  }

  override def rollback(mutaction: DeleteScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.createScalarListTable(
      mutaction.project,
      model = mutaction.model,
      fieldName = mutaction.field.dbName,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }
}

case class UpdateScalarListInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateScalarListTable] {
  override def execute(mutaction: UpdateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    val oldField = mutaction.oldField
    val newField = mutaction.newField
    val project  = mutaction.project
    val oldModel = mutaction.oldModel
    val newModel = mutaction.newModel
    builder.renameTable(project, s"${oldModel.dbName}_${oldField.dbName}", s"${newModel.dbName}_${newField.dbName}")
  }

  override def rollback(mutaction: UpdateScalarListTable, schemaBeforeMigration: DatabaseSchema) = {
    val oppositeMutaction = UpdateScalarListTable(
      mutaction.project,
      oldModel = mutaction.newModel,
      newModel = mutaction.oldModel,
      oldField = mutaction.newField,
      newField = mutaction.oldField
    )
    execute(oppositeMutaction, schemaBeforeMigration)
  }
}
