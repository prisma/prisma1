package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class NestedConnectInterpreter(mutaction: NestedConnect)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  val topIsCreate            = mutaction.topIsCreate
  val where                  = mutaction.where
  override def relationField = mutaction.relationField

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    SequenceAction(Vector(requiredCheck(parentId), removalAction(parentId), addAction(parentId))).map(_ => MutactionResults(Vector.empty))
  }

  def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = topIsCreate match {
    case false =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => requiredRelationViolation
        case (false, true, false, false)  => checkForOldParentByChildWhere(where)
        case (false, false, false, true)  => checkForOldChild(parentId)
        case (false, false, false, false) => noCheckRequired
        case (true, false, false, true)   => noCheckRequired
        case (true, false, false, false)  => noCheckRequired
        case (false, true, true, false)   => noCheckRequired
        case (false, false, true, false)  => noCheckRequired
        case (true, false, true, false)   => noCheckRequired
        case _                            => errorBecauseManySideIsRequired
      }
    case true =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => requiredRelationViolation
        case (false, true, false, false)  => checkForOldParentByChildWhere(where)
        case (false, false, false, true)  => noActionRequired
        case (false, false, false, false) => noActionRequired
        case (true, false, false, true)   => noActionRequired
        case (true, false, false, false)  => noActionRequired
        case (false, true, true, false)   => noActionRequired
        case (false, false, true, false)  => noActionRequired
        case (true, false, true, false)   => noActionRequired
        case _                            => errorBecauseManySideIsRequired
      }
  }

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = topIsCreate match {
    case false =>
      (p.isList, c.isList) match {
        case (false, false) => SequenceAction(Vector(removalByParent(parentId), removalByChild))
        case (true, false)  => removalByChild
        case (false, true)  => removalByParent(parentId)
        case (true, true)   => noActionRequired
      }
    case true =>
      (p.isList, c.isList) match {
        case (false, false) => removalByChild
        case (true, false)  => removalByChild
        case (false, true)  => noActionRequired
        case (true, true)   => noActionRequired
      }
  }

  def removalByChild(implicit mutationBuilder: MongoActionsBuilder) = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
          }
    } yield ()
  }

  def checkForOldParentByChildWhere(childWhere: NodeSelector)(implicit mutationBuilder: MongoActionsBuilder) = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(childWhere)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(childWhere)
            case Some(childId) => mutationBuilder.ensureThatNodeIsNotConnected(relationField.relatedField, childId) //switch this around again?
          }
    } yield ()
  }

  def addAction(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.createRelation(mutaction.relationField, parentId, childId)
          }
    } yield ()
  }

}
