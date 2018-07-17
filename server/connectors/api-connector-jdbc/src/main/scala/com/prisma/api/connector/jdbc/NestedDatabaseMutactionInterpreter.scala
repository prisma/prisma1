package com.prisma.api.connector.jdbc

import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGCValue
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait DatabaseMutactionInterpreter {
  protected val unitResult = DBIO.successful(UnitDatabaseMutactionResult)

  protected def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty

  protected def applyErrorMapper(action: DBIO[DatabaseMutactionResult])(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    action.asTry.flatMap {
      case Success(x) => DBIO.successful(x)
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => DBIO.failed(mappedError)
          case None              => DBIO.failed(e)
        }
    }
  }
}

trait TopLevelDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {

  def dbioActionWithErrorMapped(
      mutationBuilder: JdbcActionsBuilder
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    applyErrorMapper(dbioAction(mutationBuilder))
  }

  protected def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult]

}

trait NestedDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {
  def dbioActionWithErrorMapped(
      mutationBuilder: JdbcActionsBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    applyErrorMapper(dbioAction(mutationBuilder, parentId))
  }

  protected def dbioAction(
      mutationBuilder: JdbcActionsBuilder,
      parentId: IdGCValue
  ): DBIO[DatabaseMutactionResult]
}
