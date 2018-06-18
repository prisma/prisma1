package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mysql.database.MySqlDeployDatabaseMutationBuilder
import slick.dbio.DBIOAction

object CreateProjectInterpreter extends SqlMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    MySqlDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: CreateProject) = {
    MySqlDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object TruncateProjectInterpreter extends SqlMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    MySqlDeployDatabaseMutationBuilder.truncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    DBIOAction.successful(())
  }
}

object DeleteProjectInterpreter extends SqlMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    MySqlDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }

  override def rollback(mutaction: DeleteProject) = {
    MySqlDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
