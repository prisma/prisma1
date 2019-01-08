package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import slick.dbio.DBIOAction

case class CreateProjectInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject, schemaBeforeMigration: Tables) = {
    builder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject, schemaBeforeMigration: Tables) = {
    builder.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

case class TruncateProjectInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject, schemaBeforeMigration: Tables) = {
    builder.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject, schemaBeforeMigration: Tables) = {
    DBIOAction.successful(())
  }
}

case class DeleteProjectInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject, schemaBeforeMigration: Tables) = {
    builder.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject, schemaBeforeMigration: Tables) = {
    builder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
