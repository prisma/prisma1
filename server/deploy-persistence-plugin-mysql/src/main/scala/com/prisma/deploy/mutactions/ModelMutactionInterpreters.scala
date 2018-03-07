package com.prisma.deploy.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.deploy.migration.mutactions.{ClientSqlStatementResult, CreateModelTable, DeleteModelTable, RenameTable}
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

object CreateModelInterpreter extends SqlMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable) = {
    DatabaseMutationBuilder.createTable(projectId = mutaction.projectId, name = mutaction.model)
  }

  override def rollback(mutaction: CreateModelTable) = Some {
    DatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
  }
}

object DeleteModelInterpreter extends SqlMutactionInterpreter[DeleteModelTable] {
  // TODO: this is not symmetric

  override def execute(mutaction: DeleteModelTable) = {
    val dropTable            = DatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
    val dropScalarListFields = mutaction.scalarListFields.map(field => DatabaseMutationBuilder.dropScalarListTable(mutaction.projectId, mutaction.model, field))

    DBIO.seq(dropScalarListFields :+ dropTable: _*)
  }

  override def rollback(mutaction: DeleteModelTable) = Some {
    DatabaseMutationBuilder.createTable(projectId = mutaction.projectId, name = mutaction.model)
  }
}

object RenameModelInterpreter extends SqlMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = Some(setName(mutaction, mutaction.nextName, mutaction.previousName))

  private def setName(mutaction: RenameTable, previousName: String, nextName: String): DBIOAction[Any, NoStream, Effect.All] = {
    val changeModelTableName = DatabaseMutationBuilder.renameTable(projectId = mutaction.projectId, name = previousName, newName = nextName)
    val changeScalarListFieldTableNames = mutaction.scalarListFieldsNames.map { fieldName =>
      DatabaseMutationBuilder.renameScalarListTable(mutaction.projectId, previousName, fieldName, nextName, fieldName)
    }

    DBIO.seq(changeScalarListFieldTableNames :+ changeModelTableName: _*)
  }
}
