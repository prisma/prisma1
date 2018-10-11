package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.DeleteRelationTable
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object DeleteRelationInterpreter extends MongoMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    MongoDeployDatabaseMutationBuilder.dropTable(projectId = mutaction.projectId, tableName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable) = NoAction.unit
}
