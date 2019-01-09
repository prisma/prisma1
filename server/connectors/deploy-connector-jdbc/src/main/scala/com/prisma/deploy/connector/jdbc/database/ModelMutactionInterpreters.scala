package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable, DatabaseSchema}
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.PostgresProfile.api._

case class CreateModelInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateModelTable] {

  override def execute(mutaction: CreateModelTable, schemaBeforeMigration: DatabaseSchema): DBIOAction[Any, NoStream, Effect.All] = {
    schemaBeforeMigration.table(mutaction.model.dbName) match {
      case None =>
        builder.createModelTable(projectId = mutaction.projectId, model = mutaction.model)
      case Some(_) =>
        DBIO.successful(())
    }
  }

  override def rollback(mutaction: CreateModelTable, schemaBeforeMigration: DatabaseSchema) = {
    // only drop the table if it was created in the step before
    schemaBeforeMigration.table(mutaction.model.dbName) match {
      case None =>
        builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model.dbName)
      case Some(_) =>
        DBIO.successful(())
    }
  }
}

case class DeleteModelInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteModelTable] {
  // TODO: this is not symmetric

  override def execute(mutaction: DeleteModelTable, schemaBeforeMigration: DatabaseSchema) = {
    val droppingTable = schemaBeforeMigration.table(mutaction.model.dbName) match {
      case Some(_) =>
        builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model.dbName)
      case None =>
        DBIO.successful(())
    }

    val dropScalarListFields = mutaction.scalarListFields.map { field =>
      builder.dropScalarListTable(mutaction.projectId, mutaction.model.dbName, field, schemaBeforeMigration)
    }

    DBIO.seq(dropScalarListFields :+ droppingTable: _*)
  }

  override def rollback(mutaction: DeleteModelTable, schemaBeforeMigration: DatabaseSchema) = builder.createModelTable(
    projectId = mutaction.projectId,
    model = mutaction.model
  )
}

case class RenameModelInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable, schemaBeforeMigration: DatabaseSchema)  = setName(mutaction, mutaction.previousName, mutaction.nextName)
  override def rollback(mutaction: RenameTable, schemaBeforeMigration: DatabaseSchema) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String): DBIOAction[Any, NoStream, Effect.All] = {
    val changeModelTableName = builder.renameTable(projectId = mutaction.projectId, currentName = previousName, newName = nextName)
    val changeScalarListFieldTableNames = mutaction.scalarListFieldsNames.map { fieldName =>
      builder.renameScalarListTable(mutaction.projectId, previousName, fieldName, nextName, fieldName)
    }

    DBIO.seq(changeScalarListFieldTableNames :+ changeModelTableName: _*)
  }
}
