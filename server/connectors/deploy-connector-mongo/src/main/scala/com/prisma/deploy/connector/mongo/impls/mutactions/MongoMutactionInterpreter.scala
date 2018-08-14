package com.prisma.deploy.connector.mongo.impls.mutactions

import com.prisma.deploy.connector.DeployMutaction

trait MongoMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T): DeployMongoAction
  def rollback(mutaction: T): DeployMongoAction
}
