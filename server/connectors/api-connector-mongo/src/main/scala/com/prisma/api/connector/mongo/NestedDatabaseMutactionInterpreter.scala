package com.prisma.api.connector.mongo

import com.prisma.api.connector.MutactionResults
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder}
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait DatabaseMutactionInterpreter {
  protected def errorMapper: PartialFunction[Throwable, UserFacingError] = PartialFunction.empty

  protected def applyErrorMapper(action: MongoAction[MutactionResults])(implicit ec: ExecutionContext) = {
    action.asTry.flatMap {
      case Success(x) => MongoAction.successful(x)
      case Failure(e) =>
        errorMapper.lift(e) match {
          case Some(mappedError) => MongoAction.failed(mappedError)
          case None              => MongoAction.failed(e)
        }
    }
  }
}

trait TopLevelDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {

  def mongoActionWithErrorMapped(mutationBuilder: MongoActionsBuilder)(implicit ec: ExecutionContext) = {
    applyErrorMapper(mongoAction(mutationBuilder))
  }

  protected def mongoAction(mutationBuilder: MongoActionsBuilder): MongoAction[MutactionResults]
}

trait NestedDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {

  def mongoActionWithErrorMapped(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    applyErrorMapper(mongoAction(mutationBuilder, parentId))
  }

  protected def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue): MongoAction[MutactionResults]
}
