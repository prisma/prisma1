package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class NestedDisconnectInterpreter(mutaction: NestedDisconnect)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    SequenceAction(Vector(requiredCheck(parentId), removalAction(parentId))).map(_ => MutactionResults(Vector.empty))
  }

  def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = {
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
      case _                            => errorBecauseManySideIsRequired
    }
  }

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = {
    mutaction.where match {
      case None =>
        for {
          _ <- mutationBuilder.ensureThatParentIsConnected(mutaction.relationField, parentId)
          _ <- mutationBuilder.deleteRelationRowByParentId(mutaction.relationField, parentId)
        } yield ()

      case Some(where) =>
        for {
          id <- mutationBuilder.getNodeIdByWhere(where)
          _ <- id match {
                case None => throw APIErrors.NodeNotFoundForWhereError(where)
                case Some(childId) =>
                  for {
                    _ <- mutationBuilder.ensureThatNodesAreConnected(mutaction.relationField, childId, parentId)
                    _ <- mutationBuilder.deleteRelationRowByChildIdAndParentId(mutaction.relationField, childId, parentId)
                  } yield ()
              }
        } yield ()
    }
  }
}
