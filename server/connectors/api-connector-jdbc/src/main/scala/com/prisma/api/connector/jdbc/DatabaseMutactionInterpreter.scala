package com.prisma.api.connector.jdbc

import com.prisma.api.connector.jdbc.database.JdbcApiDatabaseMutationBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, UnitDatabaseMutactionResult}
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGCValue
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait DatabaseMutactionInterpreter {
  protected val unitResult = DBIO.successful(UnitDatabaseMutactionResult)

  def dbioActionWithErrorMapped(
      mutationBuilder: JdbcApiDatabaseMutationBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    dbioAction(mutationBuilder, parentId).asTry.flatMap {
      case Success(x) => DBIO.successful(x)
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => DBIO.failed(mappedError)
          case None              => DBIO.failed(e)
        }
    }
  }

  protected def dbioAction(
      mutationBuilder: JdbcApiDatabaseMutationBuilder,
      parentId: IdGCValue
  ): DBIO[DatabaseMutactionResult]

  protected def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty
}
