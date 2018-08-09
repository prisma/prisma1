package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{SimpleMongoAction, MongoActionsBuilder}

import scala.concurrent.ExecutionContext

case class CreateNodeInterpreter(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[CreateNodeResult] = {
    mutationBuilder.createNode(mutaction, includeRelayRow)
  }
}
