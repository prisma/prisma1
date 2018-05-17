package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector.postgresql.database.PostgresDeployDatabaseMutationBuilder
import com.prisma.deploy.connector.{CreateInlineRelation, CreateRelationTable, DeleteRelationTable}

object CreateRelationInterpreter extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable) = {
    val modelA = mutaction.schema.getModelById_!(mutaction.relation.modelAId)
    val modelB = mutaction.schema.getModelById_!(mutaction.relation.modelBId)

    PostgresDeployDatabaseMutationBuilder.createRelationTable(
      projectId = mutaction.projectId,
      relationTableName = mutaction.relation.relationTableName,
      modelA = modelA,
      modelB = modelB
    )
  }

  override def rollback(mutaction: CreateRelationTable) = {
    PostgresDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }
}

object DeleteRelationInterpreter extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    PostgresDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable) = {
    val createRelation = CreateRelationTable(mutaction.projectId, mutaction.schema, mutaction.relation)
    CreateRelationInterpreter.execute(createRelation)
  }
}

object CreateInlineRelationInterpreter extends SqlMutactionInterpreter[CreateInlineRelation] {
  override def execute(mutaction: CreateInlineRelation) = {
    PostgresDeployDatabaseMutationBuilder.createRelationColumn(mutaction.projectId, mutaction.model, mutaction.field, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: CreateInlineRelation) = ???
}
