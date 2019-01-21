package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import slick.dbio.DBIOAction

case class TruncateProjectInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject, schemaBeforeMigration: DatabaseSchema) = {
    builder.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject, schemaBeforeMigration: DatabaseSchema) = {
    DBIOAction.successful(())
  }
}
