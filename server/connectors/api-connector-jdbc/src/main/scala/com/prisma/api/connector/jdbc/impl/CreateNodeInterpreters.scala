package com.prisma.api.connector.jdbc.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, RootGCValue}
import com.prisma.shared.models.RelationField
import org.postgresql.util.PSQLException
import slick.dbio._

import scala.concurrent.ExecutionContext

case class CreateNodeInterpreter(
    mutaction: CreateNode,
    includeRelayRow: Boolean
)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter {
  val model = mutaction.model

  override def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult] = {
    for {
      id <- mutationBuilder.createNode(model, mutaction.nonListArgs)
      _  <- mutationBuilder.createScalarListValuesForNodeId(model, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayId(model, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.project, mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.project, mutaction.model, e).get)

    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("")

    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)
  }

}

case class NestedCreateNodeInterpreter(
    mutaction: NestedCreateNode,
    includeRelayRow: Boolean
)(implicit val ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with NestedRelationInterpreterBase {

  override def relationField = mutaction.relationField
  val relatedModel           = relationField.relatedModel_!

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    for {
      _  <- DBIO.seq(requiredCheck(parentId), removalAction(parentId))
      id <- createNodeAndConnectToParent(relationField, mutationBuilder, parentId)
      _  <- mutationBuilder.createScalarListValuesForNodeId(relatedModel, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayId(relatedModel, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  private def createNodeAndConnectToParent(
      relationField: RelationField,
      mutationBuilder: JdbcActionsBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext) = {
    relationField.relatedField.relationIsInlinedInParent match {
      case true =>
        val argsMap      = mutaction.nonListArgs.raw.asRoot.map
        val modifiedArgs = argsMap.updated(relationField.relatedField.name, parentId)
        mutationBuilder.createNode(relatedModel, PrismaArgs(RootGCValue(modifiedArgs)))
      case false =>
        for {
          id <- mutationBuilder.createNode(relatedModel, mutaction.nonListArgs)
          _  <- mutationBuilder.createRelation(relationField, parentId, id)
        } yield id

    }
  }

  def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] = {
    mutaction.topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => noCheckRequired
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
        noCheckRequired
    }
  }

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[_] =
    mutaction.topIsCreate match {
      case false =>
        (p.isList, c.isList) match {
          case (false, false) => removalByParent(parentId)
          case (true, false)  => noActionRequired
          case (false, true)  => removalByParent(parentId)
          case (true, true)   => noActionRequired
        }

      case true =>
        noActionRequired
    }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.project, relatedModel, e).isDefined =>
      APIErrors.UniqueConstraintViolation(relatedModel.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.project, relatedModel, e).get)

    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("")

    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(relatedModel.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)
  }
}
