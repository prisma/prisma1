package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._

case class CreateRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.createRelationTable(mutaction.project, mutaction.relation)
  }

  override def rollback(mutaction: CreateRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.dropTable(mutaction.project, tableName = mutaction.relation.relationTableName)
  }
}

case class DeleteRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.dropTable(mutaction.project, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    val createRelation = CreateRelationTable(mutaction.project, mutaction.relation)
    CreateRelationInterpreter(builder).execute(createRelation, schemaBeforeMigration)
  }
}

case class UpdateRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateRelationTable] {
  override def execute(mutaction: UpdateRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.updateRelationTable(mutaction.project, previousRelation = mutaction.previousRelation, nextRelation = mutaction.nextRelation)
  }

  override def rollback(mutaction: UpdateRelationTable, schemaBeforeMigration: DatabaseSchema) = {
    builder.updateRelationTable(mutaction.project, previousRelation = mutaction.nextRelation, nextRelation = mutaction.previousRelation)
  }
}

case class CreateInlineRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateInlineRelation] {
  override def execute(mutaction: CreateInlineRelation, schemaBeforeMigration: DatabaseSchema) = {
    builder.createRelationColumn(mutaction.project, mutaction.model, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: CreateInlineRelation, schemaBeforeMigration: DatabaseSchema) = {
    DeleteInlineRelationInterpreter(builder).execute(
      DeleteInlineRelation(mutaction.project, mutaction.relation, mutaction.model, mutaction.references, mutaction.column),
      schemaBeforeMigration
    )
  }
}

case class DeleteInlineRelationInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteInlineRelation] {
  override def execute(mutaction: DeleteInlineRelation, schemaBeforeMigration: DatabaseSchema) = {
    builder.deleteRelationColumn(mutaction.project, mutaction.model, mutaction.references, mutaction.column)
  }

  override def rollback(mutaction: DeleteInlineRelation, schemaBeforeMigration: DatabaseSchema) = {
    CreateInlineRelationInterpreter(builder).execute(
      CreateInlineRelation(mutaction.project, mutaction.relation, mutaction.model, mutaction.references, mutaction.column),
      schemaBeforeMigration
    )
  }
}
