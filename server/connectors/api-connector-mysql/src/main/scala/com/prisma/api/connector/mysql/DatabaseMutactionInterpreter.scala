package com.prisma.api.connector.mysql

import com.prisma.api.schema.GeneralError
import slick.dbio.{DBIOAction, Effect, NoStream}

trait DatabaseMutactionInterpreter {
  def action: DBIOAction[Any, NoStream, Effect.All]

  def errorMapper: PartialFunction[Throwable, GeneralError] = PartialFunction.empty
}
