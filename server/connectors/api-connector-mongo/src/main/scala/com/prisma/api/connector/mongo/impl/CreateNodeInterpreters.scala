package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SimpleMongoAction}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class CreateNodeInterpreter(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.createNode(mutaction, includeRelayRow)
  }
}

case class NestedCreateNodeInterpreter(mutaction: NestedCreateNode, includeRelayRow: Boolean)(implicit val ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue) = {
    mutationBuilder.nestedCreateNode(mutaction, parentId)
  }
}
