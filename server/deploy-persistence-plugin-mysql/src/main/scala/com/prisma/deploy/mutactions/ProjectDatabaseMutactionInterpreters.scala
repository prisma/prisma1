package com.prisma.deploy.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.deploy.migration.mutactions._

object CreateClientDatabaseForProjectInterpreter extends SqlMutactionInterpreter[CreateClientDatabaseForProject] {
  override def execute(mutaction: CreateClientDatabaseForProject) =
    DatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)

  override def rollback(mutaction: CreateClientDatabaseForProject) = Some {
    DatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)
  }
}

object DeleteClientDatabaseForProjectInterpreter extends SqlMutactionInterpreter[DeleteClientDatabaseForProject] {
  override def execute(mutaction: DeleteClientDatabaseForProject) =
    DatabaseMutationBuilder.deleteProjectDatabase(projectId = mutaction.projectId)

  override def rollback(mutaction: DeleteClientDatabaseForProject) = Some {
    DatabaseMutationBuilder.createClientDatabaseForProject(projectId = mutaction.projectId)
  }
}
