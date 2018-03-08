package com.prisma.deploy.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.deploy.migration.mutactions.{CreateRelationTable, DeleteRelationTable}

object CreateRelationInterpreter extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable) = {
    val aModel = mutaction.schema.getModelById_!(mutaction.relation.modelAId)
    val bModel = mutaction.schema.getModelById_!(mutaction.relation.modelBId)

    DatabaseMutationBuilder.createRelationTable(
      projectId = mutaction.projectId,
      tableName = mutaction.relation.id,
      aTableName = aModel.name,
      bTableName = bModel.name
    )
  }

  override def rollback(mutaction: CreateRelationTable) = {
    DatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.id)
  }
}

object DeleteRelationInterpreter extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    DatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.id)
  }

  override def rollback(mutaction: DeleteRelationTable) = {
    val createRelation = CreateRelationTable(mutaction.projectId, mutaction.schema, mutaction.relation)
    CreateRelationInterpreter.execute(createRelation)
  }
}
