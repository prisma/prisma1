package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector.{ModelEdge, NestedConnectRelation, Path, VerifyConnection}
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import slick.dbio.{DBIO, Effect, NoStream}
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class NestedConnectRelationInterpreter(mutaction: NestedConnectRelation)(implicit ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def path        = Path.empty(mutaction.where).append(ModelEdge(mutaction.relationField))
  override def project     = mutaction.project
  override def topIsCreate = mutaction.topIsCreate

  override def requiredCheck(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[_]] = {
    val x = topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(checkForOldParentByChildWhere)
          case (false, false, false, true)  => List(checkForOldChild)
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
          case (false, true, false, false)  => List(checkForOldParentByChildWhere)
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
    val verifyConnnection = VerifyConnectionInterpreter(VerifyConnection(project, path)).actionWithErrorMapped(mutationBuilder)

    verifyConnnection +: x
  }

  override def removalActions(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] =
    topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(removalByParent)
          case (false, false, false, true)  => List(removalByParent, removalByChildWhere)
          case (false, false, false, false) => List(removalByParent, removalByChildWhere)
          case (true, false, false, true)   => List(removalByChildWhere)
          case (true, false, false, false)  => List(removalByChildWhere)
          case (false, true, true, false)   => List(removalByParent)
          case (false, false, true, false)  => List(removalByParent)
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }
      case true =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => noActionRequired
          case (false, false, false, true)  => List(removalByChildWhere)
          case (false, false, false, false) => List(removalByChildWhere)
          case (true, false, false, true)   => List(removalByChildWhere)
          case (true, false, false, false)  => List(removalByChildWhere)
          case (false, true, true, false)   => noActionRequired
          case (false, false, true, false)  => noActionRequired
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }
    }

  override def addAction(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] = {
    val action = for {
      id <- mutationBuilder.queryIdFromWhere(mutaction.where)
      _ <- id match {
            case None          => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
            case Some(childId) => mutationBuilder.createRelationRowByPath(mutaction.relationField, parentId, childId)
          }
    } yield ()
    List(action)
  }
}
