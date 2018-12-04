package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

case class NestedConnectInterpreter(mutaction: NestedConnect)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  val topIsCreate            = mutaction.topIsCreate
  val where                  = mutaction.where
  override def relationField = mutaction.relationField

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    DBIOAction
      .seq(requiredCheck(parentId), removalAction(parentId), addAction(parentId))
      .andThen(unitResult)
  }

  def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[_] = {
    topIsCreate match {
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
  }

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[_] =
    topIsCreate match {
      case false =>
        (p.isList, c.isList) match {
          case (false, false) => DBIO.seq(removalByParent(parentId), removalByChild)
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

  def removalByChild(implicit mutationBuilder: JdbcActionsBuilder) = {
    val action = for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
          }
    } yield ()
    action
  }

  def checkForOldParentByChildWhere(childWhere: NodeSelector)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(childWhere)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(childWhere)
            case Some(childId) => mutationBuilder.ensureThatNodeIsNotConnected(relationField, childId)
          }
    } yield ()
  }

  def addAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.createRelation(mutaction.relationField, parentId, childId)
          }
    } yield ()
  }

}
