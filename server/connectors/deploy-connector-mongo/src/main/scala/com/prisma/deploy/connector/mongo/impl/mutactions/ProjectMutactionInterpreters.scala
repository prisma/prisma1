package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object CreateProjectInterpreter extends MongoMutactionInterpreter[CreateProject] {
  override def execute(mutaction: CreateProject) = {
    MongoDeployDatabaseMutationBuilder.createClientDatabaseForProject
  }

  override def rollback(mutaction: CreateProject) = {
    MongoDeployDatabaseMutationBuilder.deleteProjectDatabase
  }
}

object TruncateProjectInterpreter extends MongoMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    MongoDeployDatabaseMutationBuilder.nonDestructiveTruncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    NoAction.unit
  }
}

object DeleteProjectInterpreter extends MongoMutactionInterpreter[DeleteProject] {
  override def execute(mutaction: DeleteProject) = {
    MongoDeployDatabaseMutationBuilder.deleteProjectDatabase
  }

  override def rollback(mutaction: DeleteProject) = {
    MongoDeployDatabaseMutationBuilder.createClientDatabaseForProject
  }
}
