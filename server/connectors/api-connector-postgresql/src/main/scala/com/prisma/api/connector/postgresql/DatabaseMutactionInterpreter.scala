package com.prisma.api.connector.postgresql

import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

trait DatabaseMutactionInterpreter {
  private val unitResult = DBIO.successful(UnitDatabaseMutactionResult)

  def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[DatabaseMutactionResult] = action(mutationBuilder).andThen(unitResult)

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIOAction[Any, NoStream, Effect.All]

  def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty
}
