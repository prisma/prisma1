package com.prisma.api.connector.postgresql

import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGcValue
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait DatabaseMutactionInterpreter {
  private val unitResult = DBIO.successful(UnitDatabaseMutactionResult)

  // FIXME: only new action should be implemented by subclasses (make protected);
  // FIXME: and only newActionWithErrorMapped should be called from the outside

  def newActionWithErrorMapped(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGcValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    newAction(mutationBuilder, parentId).asTry.map {
      case Success(x) => x
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => throw mappedError
          case None              => throw e
        }
    }
  }

  def newAction(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGcValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    actionWithErrorMapped(mutationBuilder).andThen(unitResult)
  }

  def actionWithErrorMapped(mutationBuilder: PostgresApiDatabaseMutationBuilder)(implicit ec: ExecutionContext): DBIO[_] = {
    action(mutationBuilder).asTry.map {
      case Success(x) => x
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => throw mappedError
          case None              => throw e
        }
    }
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIOAction[Any, NoStream, Effect.All]

  def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty
}
