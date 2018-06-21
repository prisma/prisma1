package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector.{ModelEdge, NestedDisconnectRelation, NodeSelector, Path}
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, IntGCValue}

import scala.concurrent.ExecutionContext

case class NestedDisconnectRelationInterpreter(mutaction: NestedDisconnectRelation)(implicit ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

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
