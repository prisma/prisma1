package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class NestedConnectRelationInterpreter(mutaction: NestedConnectRelation)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  def topIsCreate            = mutaction.topIsCreate
  val where                  = mutaction.where
  override def relationField = mutaction.relationField

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    DBIOAction.seq(allActions(mutationBuilder, parentId): _*).andThen(DBIO.successful(UnitDatabaseMutactionResult))
  }

  override def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[_]] = {
    topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(checkForOldParentByChildWhere(where))
          case (false, false, false, true)  => List(checkForOldChild(parentId))
          case (false, false, false, false) => noCheckRequired
          case (true, false, false, true)   => noCheckRequired
          case (true, false, false, false)  => noCheckRequired
          case (false, true, true, false)   => noCheckRequired
          case (false, false, true, false)  => noCheckRequired
          case (true, false, true, false)   => noCheckRequired
          case _                            => sysError
        }
      case true =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(checkForOldParentByChildWhere(where))
          case (false, false, false, true)  => noActionRequired
          case (false, false, false, false) => noActionRequired
          case (true, false, false, true)   => noActionRequired
          case (true, false, false, false)  => noActionRequired
          case (false, true, true, false)   => noActionRequired
          case (false, false, true, false)  => noActionRequired
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }
    }
  }

  override def removalActions(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] =
    topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(removalByParent(parentId))
          case (false, false, false, true)  => List(removalByParent(parentId), removalByChild)
          case (false, false, false, false) => List(removalByParent(parentId), removalByChild)
          case (true, false, false, true)   => List(removalByChild)
          case (true, false, false, false)  => List(removalByChild)
          case (false, true, true, false)   => List(removalByParent(parentId))
          case (false, false, true, false)  => List(removalByParent(parentId))
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }
      case true =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => noActionRequired
          case (false, false, false, true)  => List(removalByChild)
          case (false, false, false, false) => List(removalByChild)
          case (true, false, false, true)   => List(removalByChild)
          case (true, false, false, false)  => List(removalByChild)
          case (false, true, true, false)   => noActionRequired
          case (false, false, true, false)  => noActionRequired
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }
    }

  def removalByChild(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val action = for {
      id <- mutationBuilder.queryIdFromWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
          }
    } yield ()
    action
  }

  def checkForOldParentByChildWhere(where: NodeSelector)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[Unit] = {
    for {
      id <- mutationBuilder.queryIdFromWhere(where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(where)
            case Some(childId) => mutationBuilder.ensureThatNodeIsNotConnected(relationField, childId)
          }
    } yield ()
  }

  override def addAction(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] = {
    val action = for {
      id <- mutationBuilder.queryIdFromWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.createRelation(mutaction.relationField, parentId, childId)
          }
    } yield ()
    List(action)
  }

}
