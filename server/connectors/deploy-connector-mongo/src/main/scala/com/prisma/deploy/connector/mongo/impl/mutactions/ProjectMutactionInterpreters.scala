package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object TruncateProjectInterpreter extends MongoMutactionInterpreter[TruncateProject] {
  override def execute(mutaction: TruncateProject) = {
    MongoDeployDatabaseMutationBuilder.nonDestructiveTruncateProjectTables(project = mutaction.project)
  }

  override def rollback(mutaction: TruncateProject) = {
    NoAction.unit
  }
}
