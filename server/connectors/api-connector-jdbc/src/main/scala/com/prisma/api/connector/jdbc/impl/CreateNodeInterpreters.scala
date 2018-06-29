package com.prisma.api.connector.jdbc.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, RootGCValue}
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import org.postgresql.util.PSQLException

import scala.concurrent.ExecutionContext
import slick.dbio._

case class CreateNodeInterpreter(
    mutaction: CreateNode,
    includeRelayRow: Boolean = true
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionInterpreter {
  val model = mutaction.model

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[DatabaseMutactionResult] = {
    for {
      id <- mutationBuilder.createNode(model, mutaction.nonListArgs)
      _  <- mutationBuilder.setScalarListValuesByNodeId(model, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayId(model, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.model, e).get)
    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("")
    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 &&
          GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)
  }

}

case class NestedCreateNodeInterpreter(
    mutaction: NestedCreateNode,
    includeRelayRow: Boolean = true
)(implicit val ec: ExecutionContext)
    extends DatabaseMutactionInterpreter
    with NestedRelationInterpreterBase {

  override def relationField = mutaction.relationField
  val model                  = relationField.relatedModel_!

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    implicit val implicitMb = mutationBuilder
    for {
      _  <- DBIO.seq(requiredCheck(parentId), removalAction(parentId))
      id <- createNodeAndConnectToParent(mutationBuilder, parentId)
      _  <- mutationBuilder.setScalarListValuesByNodeId(model, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayId(model, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  private def createNodeAndConnectToParent(
      mutationBuilder: JdbcActionsBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext) = {
    relation.manifestation match {
      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == model.name =>
        val inlineField  = relation.getFieldOnModel(model.name)
        val argsMap      = mutaction.nonListArgs.raw.asRoot.map
        val modifiedArgs = argsMap.updated(inlineField.name, parentId)
        mutationBuilder.createNode(model, PrismaArgs(RootGCValue(modifiedArgs)))
      case _ =>
        for {
          id <- mutationBuilder.createNode(model, mutaction.nonListArgs)
          _  <- mutationBuilder.createRelation(mutaction.relationField, parentId, id)
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

  def removalAction(parentId: IdGCValue)(implicit mutationBuilder: JdbcActionsBuilder): DBIO[Unit] =
    mutaction.topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => removalByParent(parentId)
          case (false, false, false, true)  => removalByParent(parentId)
          case (false, false, false, false) => removalByParent(parentId)
          case (true, false, false, true)   => noActionRequired
          case (true, false, false, false)  => noActionRequired
          case (false, true, true, false)   => removalByParent(parentId)
          case (false, false, true, false)  => removalByParent(parentId)
          case (true, false, true, false)   => noActionRequired
          case _                            => errorBecauseManySideIsRequired
        }

      case true =>
        noActionRequired
    }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(model, e).get)
    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("")
    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 &&
          GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)
  }
}
