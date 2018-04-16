package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.postgresql.database.DeployDatabaseMutationBuilder
import slick.dbio.DBIOAction

object CreateProjectInterpreter extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object TruncateProjectInterpreter extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    DeployDatabaseMutationBuilder.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    DBIOAction.successful(())
  }
}

object DeleteProjectInterpreter extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
