package com.prisma.api.connector.jdbc.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models.Model
import org.postgresql.util.PSQLException
import slick.dbio._

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val model             = mutaction.where.model
  val nonListArgs       = mutaction.nonListArgs
  override def listArgs = mutaction.listArgs

  override def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) => doIt(mutationBuilder, node.id).andThen(DBIO.successful(node))
               case None       => DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
             }
    } yield UpdateNodeResult(node.id, node, mutaction)
  }

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(model, e).get)

    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeNotFoundForWhereError(mutaction.where)

    case e: PSQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull()

    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 &&
          GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()
  }
}

case class NestedUpdateNodeInterpreter(mutaction: NestedUpdateNode)(implicit ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val model       = mutaction.relationField.relatedModel_!
  val parent      = mutaction.relationField.model
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    for {
      _ <- verifyWhere(mutationBuilder, mutaction.where)
      idOpt <- mutaction.where match {
                case Some(where) => mutationBuilder.getNodeIdByParentIdAndWhere(mutaction.relationField, parentId, where)
                case None        => mutationBuilder.getNodeIdByParentId(mutaction.relationField, parentId)
              }
      id <- idOpt match {
             case Some(id) => doIt(mutationBuilder, id)
             case None =>
               throw APIErrors.NodesNotConnectedError(
                 relation = mutaction.relationField.relation,
                 parent = parent,
                 parentWhere = None,
                 child = model,
                 childWhere = mutaction.where
               )
           }
    } yield UpdateNodeResult(id, PrismaNode(id, RootGCValue.empty), mutaction)
  }

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(model, e).get)

    case e: PSQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull()

    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 &&
          GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(mutaction.nonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()
  }
}

trait SharedUpdateLogic {
  def model: Model
  def nonListArgs: PrismaArgs
  def listArgs: Vector[(String, ListGCValue)]

  def verifyWhere(mutationBuilder: JdbcActionsBuilder, where: Option[NodeSelector])(implicit ec: ExecutionContext) = {
    where match {
      case Some(where) =>
        for {
          id <- mutationBuilder.getNodeIdByWhere(where)
        } yield {
          if (id.isEmpty) throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        DBIO.successful(())
    }
  }

  def doIt(mutationBuilder: JdbcActionsBuilder, id: IdGCValue)(implicit ec: ExecutionContext): DBIO[IdGCValue] = {
    for {
      _ <- mutationBuilder.updateNodeById(model, id, nonListArgs)
      _ <- mutationBuilder.setScalarListValuesByNodeId(model, id, listArgs)
    } yield id
  }
}
