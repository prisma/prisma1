package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mysql.database.DeployDatabaseMutationBuilderMySql
import slick.dbio.DBIOAction

object CreateProjectInterpreter extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilderMySql.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject) = {
    DeployDatabaseMutationBuilderMySql.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object TruncateProjectInterpreter extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    DeployDatabaseMutationBuilderMySql.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    DBIOAction.successful(())
  }
}

object DeleteProjectInterpreter extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilderMySql.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject) = {
    DeployDatabaseMutationBuilderMySql.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
