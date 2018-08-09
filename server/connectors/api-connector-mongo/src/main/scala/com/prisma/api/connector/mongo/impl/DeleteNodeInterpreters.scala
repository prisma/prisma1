package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SimpleMongoAction}

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[DeleteNodeResult] = {
    mutationBuilder.deleteNode(mutaction)
  }
}
