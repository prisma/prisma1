package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.postgresql.database.DeployDatabaseMutationBuilderPostGres
import slick.dbio.DBIOAction

object CreateProjectInterpreter extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilderPostGres.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilderPostGres.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object TruncateProjectInterpreter extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    DeployDatabaseMutationBuilderPostGres.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    DBIOAction.successful(())
  }
}

object DeleteProjectInterpreter extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilderPostGres.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilderPostGres.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
