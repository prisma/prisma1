package com.prisma.api.connector.mysql

import com.prisma.api.connector.DatabaseMutaction
import slick.dbio.{DBIOAction, Effect, NoStream}

trait DatabaseMutactionInterpreter[T <: DatabaseMutaction] {
  def action(mutaction: T): DBIOAction[Any, NoStream, Effect.All]
}
