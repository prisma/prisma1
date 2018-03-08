package com.prisma.deploy.mutactions

import com.prisma.deploy.migration.mutactions.ClientSqlMutaction
import slick.dbio.{DBIOAction, Effect, NoStream}

trait SqlMutactionInterpreter[T <: ClientSqlMutaction] {
  def execute(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
  def rollback(mutaction: T): Option[DBIOAction[Any, NoStream, Effect.All]]
}
