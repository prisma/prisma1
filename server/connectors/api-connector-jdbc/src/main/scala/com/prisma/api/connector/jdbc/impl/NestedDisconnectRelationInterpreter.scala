package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, IntGCValue}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

case class NestedDisconnectRelationInterpreter(mutaction: NestedDisconnect)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

  override def dbioAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue) = {
    DBIOAction.seq(allActions(mutationBuilder, parentId): _*).andThen(DBIO.successful(UnitDatabaseMutactionResult))
  }

  override def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) =
    (p.isList, p.isRequired, c.isList, c.isRequired) match {
      case (false, true, false, true)   => requiredRelationViolation
      case (false, true, false, false)  => requiredRelationViolation
      case (false, false, false, true)  => requiredRelationViolation
      case (false, false, false, false) => noCheckRequired
      case (true, false, false, true)   => requiredRelationViolation
      case (true, false, false, false)  => noCheckRequired
      case (false, true, true, false)   => requiredRelationViolation
      case (false, false, true, false)  => noCheckRequired
      case (true, false, true, false)   => noCheckRequired
      case _                            => sysError
    }

  override def removalActions(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val action = mutaction.where match {
      case None =>
        for {
          _ <- mutationBuilder.ensureThatParentIsConnected(mutaction.relationField, parentId)
          _ <- mutationBuilder.deleteRelationRowByParentId(mutaction.relationField, parentId)
        } yield ()

      case Some(where) =>
        for {
          id <- mutationBuilder.queryIdFromWhere(where)
          _ <- id match {
                case None => throw APIErrors.NodeNotFoundForWhereError(where)
                case Some(childId) =>
                  for {
                    _ <- mutationBuilder.ensureThatNodeIsConnected(mutaction.relationField, childId)
                    _ <- mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
                  } yield ()
              }
        } yield ()
    }
    List(action)
  }

  override def addAction(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = noActionRequired
}
