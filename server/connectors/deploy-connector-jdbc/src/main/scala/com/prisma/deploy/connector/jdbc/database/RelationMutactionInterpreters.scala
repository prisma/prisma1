package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{CreateInlineRelation, CreateRelationTable, DeleteRelationTable, RenameRelationTable}

case class CreateRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable) = {
    val modelA = mutaction.relation.modelA
    val modelB = mutaction.relation.modelB

    builder.createRelationTable(
      projectId = mutaction.projectId,
      relationTableName = mutaction.relation.relationTableName,
      modelA = modelA,
      modelB = modelB
    )
  }

  override def rollback(mutaction: CreateRelationTable) = {
    builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }
}

case class DeleteRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable) = {
    val createRelation = CreateRelationTable(mutaction.projectId, mutaction.schema, mutaction.relation)
    CreateRelationInterpreter(builder).execute(createRelation)
  }
}

case class RenameRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[RenameRelationTable] {
  override def execute(mutaction: RenameRelationTable) = {
    builder.renameTable(projectId = mutaction.projectId, currentName = mutaction.previousName, newName = mutaction.nextName)
  }

  override def rollback(mutaction: RenameRelationTable) = {
    builder.renameTable(projectId = mutaction.projectId, currentName = mutaction.nextName, newName = mutaction.previousName)

  }
}

case class CreateInlineRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateInlineRelation] {
  override def execute(mutaction: CreateInlineRelation) = {
    builder.createRelationColumn(mutaction.projectId, mutaction.model, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: CreateInlineRelation) = ???
}
