package com.prisma.deploy.mutactions

import com.prisma.deploy.migration.mutactions.DeployMutaction
import slick.dbio.{DBIOAction, Effect, NoStream}

trait SqlMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
  def rollback(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
}
