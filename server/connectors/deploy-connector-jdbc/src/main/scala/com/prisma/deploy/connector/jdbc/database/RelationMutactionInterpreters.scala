package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._

case class CreateRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable, schemaBeforeMigration: Tables) = {
    builder.createRelationTable(mutaction.projectId, mutaction.relation)
  }

  override def rollback(mutaction: CreateRelationTable, schemaBeforeMigration: Tables) = {
    builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }
}

case class DeleteRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable, schemaBeforeMigration: Tables) = {
    builder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable, schemaBeforeMigration: Tables) = {
    val createRelation = CreateRelationTable(mutaction.projectId, mutaction.relation)
    CreateRelationInterpreter(builder).execute(createRelation, schemaBeforeMigration)
  }
}

case class UpdateRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateRelationTable] {
  override def execute(mutaction: UpdateRelationTable, schemaBeforeMigration: Tables) = {
    builder.updateRelationTable(mutaction.projectId, previousRelation = mutaction.previousRelation, nextRelation = mutaction.nextRelation)
  }

  override def rollback(mutaction: UpdateRelationTable, schemaBeforeMigration: Tables) = {
    builder.updateRelationTable(mutaction.projectId, previousRelation = mutaction.nextRelation, nextRelation = mutaction.previousRelation)
  }
}

case class CreateInlineRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateInlineRelation] {
  override def execute(mutaction: CreateInlineRelation, schemaBeforeMigration: Tables) = {
    builder.createRelationColumn(mutaction.projectId, mutaction.model, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: CreateInlineRelation, schemaBeforeMigration: Tables) = {
    DeleteInlineRelationInterpreter(builder).execute(
      DeleteInlineRelation(mutaction.projectId, mutaction.relation, mutaction.model, mutaction.references, mutaction.column),
      schemaBeforeMigration
    )
  }
}

case class DeleteInlineRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteInlineRelation] {
  override def execute(mutaction: DeleteInlineRelation, schemaBeforeMigration: Tables) = {
    builder.deleteRelationColumn(mutaction.projectId, mutaction.model, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: DeleteInlineRelation, schemaBeforeMigration: Tables) = {
    CreateInlineRelationInterpreter(builder).execute(
      CreateInlineRelation(mutaction.projectId, mutaction.relation, mutaction.model, mutaction.references, mutaction.column),
      schemaBeforeMigration
    )
  }
}
