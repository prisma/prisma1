package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SimpleMongoAction}
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.updateNode(mutaction)
  }
}

case class NestedUpdateNodeInterpreter(mutaction: NestedUpdateNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.nestedUpdateNode(mutaction, parentId)
  }
}
