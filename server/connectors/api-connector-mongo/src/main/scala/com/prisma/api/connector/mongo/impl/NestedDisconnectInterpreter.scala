package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction}
import com.prisma.api.schema.APIErrors

import scala.concurrent.ExecutionContext

case class NestedDisconnectInterpreter(mutaction: NestedDisconnect)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    implicit val implicitMb = mutationBuilder
    SequenceAction(Vector(requiredCheck(parent), removalAction(parent))).map(_ => MutactionResults(Vector.empty))
  }

  def requiredCheck(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = {
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

  def removalAction(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = {
    mutaction.where match {
      case None =>
        for {
          _ <- mutationBuilder.ensureThatParentIsConnected(mutaction.relationField, parent)
          _ <- mutationBuilder.deleteRelationRowByParent(mutaction.relationField, parent)
        } yield ()

      case Some(where) =>
        for {
          id <- mutationBuilder.getNodeIdByWhere(where)
          _ <- id match {
                case None => throw APIErrors.NodeNotFoundForWhereError(where)
                case Some(childId) =>
                  for {
                    _ <- mutationBuilder.ensureThatNodesAreConnected(mutaction.relationField, childId, parent)
                    _ <- mutationBuilder.deleteRelationRowByChildIdAndParentId(mutaction.relationField, childId, parent)
                  } yield ()
              }
        } yield ()
    }
  }
}
