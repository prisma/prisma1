package com.prisma.deploy.connector.postgresql.impls.mutactions

import com.prisma.deploy.connector.DeployMutaction
import slick.dbio.{DBIOAction, Effect, NoStream}

trait SqlMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
  def rollback(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
}
