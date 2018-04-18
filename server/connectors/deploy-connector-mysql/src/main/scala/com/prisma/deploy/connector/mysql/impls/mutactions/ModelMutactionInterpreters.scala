package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector.mysql.database.DeployDatabaseMutationBuilderMySql
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

object CreateModelInterpreter extends SqlMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable) = {
    DeployDatabaseMutationBuilderMySql.createTable(projectId = mutaction.projectId, name = mutaction.model)
  }

  override def rollback(mutaction: CreateModelTable) = {
    DeployDatabaseMutationBuilderMySql.dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
  }
}

object DeleteModelInterpreter extends SqlMutactionInterpreter[DeleteModelTable] {
  // TODO: this is not symmetric

  override def execute(mutaction: DeleteModelTable) = {
    val dropTable = DeployDatabaseMutationBuilderMySql.dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
    val dropScalarListFields =
      mutaction.scalarListFields.map(field => DeployDatabaseMutationBuilderMySql.dropScalarListTable(mutaction.projectId, mutaction.model, field))

    DBIO.seq(dropScalarListFields :+ dropTable: _*)
  }

  override def rollback(mutaction: DeleteModelTable) = {
    DeployDatabaseMutationBuilderMySql.createTable(projectId = mutaction.projectId, name = mutaction.model)
  }
}

object RenameModelInterpreter extends SqlMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable) = setName(mutaction, mutaction.previousName, mutaction.nextName)

  override def rollback(mutaction: RenameTable) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String): DBIOAction[Any, NoStream, Effect.All] = {
    val changeModelTableName = DeployDatabaseMutationBuilderMySql.renameTable(projectId = mutaction.projectId, name = previousName, newName = nextName)
    val changeScalarListFieldTableNames = mutaction.scalarListFieldsNames.map { fieldName =>
      DeployDatabaseMutationBuilderMySql.renameScalarListTable(mutaction.projectId, previousName, fieldName, nextName, fieldName)
    }

    DBIO.seq(changeScalarListFieldTableNames :+ changeModelTableName: _*)
  }
}
