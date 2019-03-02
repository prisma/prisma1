package com.prisma.api.connector.jdbc.impl

import java.sql.{SQLException, SQLIntegrityConstraintViolationException}

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.{APIErrors, UserFacingError}
import com.prisma.gc_values.{IdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models.{Model, Project}
import slick.dbio._

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val project           = mutaction.project
  val model             = mutaction.where.model
  val nonListArgs       = mutaction.nonListArgs
  override def listArgs = mutaction.listArgs

  override def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) => updateNodeById(mutationBuilder, node.id).andThen(DBIO.successful(node))
               case None       => DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
             }
    } yield UpdateNodeResult(node.id, node, mutaction)
  }

  override val errorMapper = errorHandler(mutaction.nonListArgs)
}

case class UpdateNodesInterpreter(mutaction: TopLevelUpdateNodes)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val project     = mutaction.project
  val model       = mutaction.model
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  def dbioAction(mutationBuilder: JdbcActionsBuilder) =
    for {
      ids        <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      groupedIds = ids.grouped(ParameterLimit.groupSize).toVector
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateNodesByIds(mutaction.model, mutaction.nonListArgs, _)): _*)
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateScalarListValuesForIds(mutaction.model, mutaction.listArgs, _)): _*)
    } yield ManyNodesResult(mutaction, ids.size)

  override val errorMapper = errorHandler(mutaction.nonListArgs)
}

case class NestedUpdateNodesInterpreter(mutaction: NestedUpdateNodes)(implicit ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val project     = mutaction.project
  val model       = mutaction.relationField.relatedModel_!
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    for {
      ids        <- mutationBuilder.getNodesIdsByParentIdAndWhereFilter(mutaction.relationField, parentId, mutaction.whereFilter)
      groupedIds = ids.grouped(ParameterLimit.groupSize).toVector
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateNodesByIds(mutaction.model, mutaction.nonListArgs, _)): _*)
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateScalarListValuesForIds(mutaction.model, mutaction.listArgs, _)): _*)
    } yield ManyNodesResult(mutaction, ids.size)
  }

  override val errorMapper = errorHandler(mutaction.nonListArgs)
}

case class NestedUpdateNodeInterpreter(mutaction: NestedUpdateNode)(implicit ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with SharedUpdateLogic {
  val project     = mutaction.project
  val model       = mutaction.relationField.relatedModel_!
  val parent      = mutaction.relationField.model
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    for {
      _ <- verifyChildWhere(mutationBuilder, mutaction.where)
      childId <- mutaction.where match {
                  case Some(where) => mutationBuilder.getNodeIdByParentIdAndWhere(mutaction.relationField, parentId, where)
                  case None        => mutationBuilder.getNodeIdByParentId(mutaction.relationField, parentId)
                }
      id <- childId match {
             case Some(id) => updateNodeById(mutationBuilder, id)
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

  override val errorMapper = errorHandler(mutaction.nonListArgs)
}

trait SharedUpdateLogic {
  def project: Project
  def model: Model
  def nonListArgs: PrismaArgs
  def listArgs: Vector[(String, ListGCValue)]

  def verifyChildWhere(mutationBuilder: JdbcActionsBuilder, where: Option[NodeSelector])(implicit ec: ExecutionContext) = {
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

  def updateNodeById(mutationBuilder: JdbcActionsBuilder, id: IdGCValue)(implicit ec: ExecutionContext): DBIO[IdGCValue] = {
    for {
      _ <- mutationBuilder.updateNodeById(model, id, nonListArgs)
      _ <- mutationBuilder.updateScalarListValuesForNodeId(model, id, listArgs)
    } yield id
  }

  def errorHandler(args: PrismaArgs): PartialFunction[Throwable, UserFacingError] = {
    case e: SQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(project, model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(project, model, e).get)

    case e: SQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull()

    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOptionMySql(nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionMySql(nonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()

    case e: SQLException if e.getErrorCode == 19 && GetFieldFromSQLUniqueException.getFieldOptionSQLite(args.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionSQLite(args.keys, e).get)
  }
}
