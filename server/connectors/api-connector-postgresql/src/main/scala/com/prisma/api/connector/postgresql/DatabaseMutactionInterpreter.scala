package com.prisma.api.connector.postgresql

import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import slick.dbio.{DBIOAction, Effect, NoStream}

trait DatabaseMutactionInterpreter {
  def action: DBIOAction[DatabaseMutactionResult, NoStream, Effect.All]

  def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty

  def mapToUnitResult(x: Any): UnitDatabaseMutactionResult.type = UnitDatabaseMutactionResult
}
