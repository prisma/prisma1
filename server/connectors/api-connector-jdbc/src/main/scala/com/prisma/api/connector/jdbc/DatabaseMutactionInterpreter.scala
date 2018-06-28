package com.prisma.api.connector.jdbc

import com.prisma.api.connector.jdbc.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGCValue
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait DatabaseMutactionInterpreter {
  protected val unitResult = DBIO.successful(UnitDatabaseMutactionResult)

  // FIXME: only new action should be implemented by subclasses (make protected);
  // FIXME: and only newActionWithErrorMapped should be called from the outside

  def newActionWithErrorMapped(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    newAction(mutationBuilder, parentId).asTry.flatMap {
      case Success(x) => DBIO.successful(x)
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => DBIO.failed(mappedError)
          case None              => DBIO.failed(e)
        }
    }
  }

  protected def newAction(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    actionWithErrorMapped(mutationBuilder).andThen(unitResult)
  }

  def actionWithErrorMapped(mutationBuilder: PostgresApiDatabaseMutationBuilder)(implicit ec: ExecutionContext): DBIO[_] = {
    action(mutationBuilder).asTry.flatMap {
      case Success(x) => DBIO.successful(x)
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => DBIO.failed(mappedError)
          case None              => DBIO.failed(e)
        }
    }
  }

  protected def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIOAction[Any, NoStream, Effect.All]

  def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty
}
