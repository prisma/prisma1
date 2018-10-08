package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.DeleteRelationTable
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

//Fixme this is wrong
object DeleteRelationInterpreter extends MongoMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable) = {
    MongoDeployDatabaseMutationBuilder.dropCollection(collectionName = mutaction.relation.relationTableName)
  }

  override def rollback(mutaction: DeleteRelationTable) = NoAction.unit
}
