package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector.postgresql.database.DeployDatabaseMutationBuilderPostGres
import com.prisma.deploy.connector.postgresql.database.DeployDatabaseMutationBuilderPostGres._
import com.prisma.deploy.connector.{CreateModelTable, DeleteModelTable, RenameTable}
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.PostgresProfile.api._

object CreateModelInterpreter extends SqlMutactionInterpreter[CreateModelTable] {
  override def execute(mutaction: CreateModelTable)  = createTable(projectId = mutaction.projectId, name = mutaction.model)
  override def rollback(mutaction: CreateModelTable) = dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
}

object DeleteModelInterpreter extends SqlMutactionInterpreter[DeleteModelTable] {
  // TODO: this is not symmetric

  override def execute(mutaction: DeleteModelTable) = {
    val droppingTable        = dropTable(projectId = mutaction.projectId, tableName = mutaction.model)
    val dropScalarListFields = mutaction.scalarListFields.map(field => dropScalarListTable(mutaction.projectId, mutaction.model, field))

    DBIO.seq(dropScalarListFields :+ droppingTable: _*)
  }

  override def rollback(mutaction: DeleteModelTable) = createTable(projectId = mutaction.projectId, name = mutaction.model)
}

object RenameModelInterpreter extends SqlMutactionInterpreter[RenameTable] {
  override def execute(mutaction: RenameTable)  = setName(mutaction, mutaction.previousName, mutaction.nextName)
  override def rollback(mutaction: RenameTable) = setName(mutaction, mutaction.nextName, mutaction.previousName)

  private def setName(mutaction: RenameTable, previousName: String, nextName: String): DBIOAction[Any, NoStream, Effect.All] = {
    val changeModelTableName = renameTable(projectId = mutaction.projectId, name = previousName, newName = nextName)
    val changeScalarListFieldTableNames =
      mutaction.scalarListFieldsNames.map(fieldName => renameScalarListTable(mutaction.projectId, previousName, fieldName, nextName, fieldName))

    DBIO.seq(changeScalarListFieldTableNames :+ changeModelTableName: _*)
  }
}
