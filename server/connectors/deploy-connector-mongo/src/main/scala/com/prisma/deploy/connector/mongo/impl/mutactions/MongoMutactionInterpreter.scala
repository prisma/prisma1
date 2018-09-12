package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.DeployMutaction
import com.prisma.deploy.connector.mongo.impl.DeployMongoAction

trait MongoMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T): DeployMongoAction
  def rollback(mutaction: T): DeployMongoAction
}
