package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector.mysql.database.MySqlDeployDatabaseMutationBuilder
import com.prisma.deploy.connector.{CreateRelationTable, DeleteRelationTable, RenameRelationTable}

object CreateRelationInterpreter extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable) = {
    val aModel = mutaction.relation.modelA
    val bModel = mutaction.relation.modelB

    MySqlDeployDatabaseMutationBuilder.createRelationTable(
      projectId = mutaction.projectId,
      tableName = mutaction.relation.relationTableName,
      aTableName = aModel.name,
      bTableName = bModel.name
    )
  }

  override def rollback(mutaction: CreateRelationTable) = {
    MySqlDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }
}

object DeleteRelationInterpreter extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    MySqlDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable) = {
    val createRelation = CreateRelationTable(mutaction.projectId, mutaction.schema, mutaction.relation)
    CreateRelationInterpreter.execute(createRelation)
  }
}

object RenameRelationInterpreter extends SqlMutactionInterpreter[RenameRelationTable] {
  override def execute(mutaction: RenameRelationTable) = {
    MySqlDeployDatabaseMutationBuilder.renameTable(projectId = mutaction.projectId, name = mutaction.previousName, newName = mutaction.nextName)
  }

  override def rollback(mutaction: RenameRelationTable) = {
    MySqlDeployDatabaseMutationBuilder.renameTable(projectId = mutaction.projectId, name = mutaction.nextName, newName = mutaction.previousName)

  }
}
