package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.postgresql.database.PostgresDeployDatabaseMutationBuilder
import slick.dbio.DBIOAction

object CreateProjectInterpreter extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    PostgresDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject) = {
    PostgresDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object TruncateProjectInterpreter extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    PostgresDeployDatabaseMutationBuilder.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    DBIOAction.successful(())
  }
}

object DeleteProjectInterpreter extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    PostgresDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject) = {
    PostgresDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
