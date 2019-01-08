package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

case class NestedSetInterpreter(mutaction: NestedSet)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    DBIOAction
      .seq(requiredCheck(parentId), removalAction(parentId), addAction(parentId))
      .andThen(unitResult)
  }

  def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[_] = noCheckRequired

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[_] =
    c.isList match {
      case false => DBIO.seq(removalByParent(parentId), removalByChild)
      case true  => removalByParent(parentId)
    }

  def removalByChild(implicit mutationBuilder: JdbcActionsBuilder) = {

    DBIO.seq(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
            }
      } yield ()
    }: _*)
  }

  def checkForOldParentByChildWhere(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] = {
    DBIO.seq(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.ensureThatNodeIsNotConnected(relationField, childId)
            }
      } yield ()
    }: _*)
  }

  def addAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] = {
    DBIO.seq(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.createRelation(mutaction.relationField, parentId, childId)
            }
      } yield ()
    }: _*)
  }

}
