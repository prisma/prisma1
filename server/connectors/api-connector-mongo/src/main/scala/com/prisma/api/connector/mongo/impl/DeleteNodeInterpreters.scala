package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SimpleMongoAction}
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.deleteNode(mutaction)
  }
}

case class NestedDeleteNodeInterpreter(mutaction: NestedDeleteNode, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.nestedDeleteNode(mutaction, parentId)
  }
}
