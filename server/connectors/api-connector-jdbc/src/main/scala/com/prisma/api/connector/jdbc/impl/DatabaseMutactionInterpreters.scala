package com.prisma.api.connector.jdbc.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.jdbc.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.{APIErrors, UserFacingError}
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models._
import org.postgresql.util.PSQLException
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

case class CreateDataItemInterpreter(mutaction: CreateNode, includeRelayRow: Boolean = true) extends DatabaseMutactionInterpreter {
  val model = mutaction.model

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(
      implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    for {
      id <- mutationBuilder.createDataItem(model, mutaction.nonListArgs)
      _  <- mutationBuilder.setScalarListById(model, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayRowById(model, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

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

case class NestedCreateDataItemInterpreter(mutaction: NestedCreateNode, includeRelayRow: Boolean = true)(implicit val ec: ExecutionContext)
    extends DatabaseMutactionInterpreter
    with NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField
  val model                  = relationField.relatedModel_!

  override def addAction(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder) = ???

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _  <- DBIO.sequence(requiredCheck(parentId)(mutationBuilder))
      _  <- DBIO.sequence(removalActions(parentId)(mutationBuilder))
      id <- createNodeAndConnectToParent(mutationBuilder, parentId)
      _  <- mutationBuilder.setScalarListById(model, id, mutaction.listArgs)
      _  <- if (includeRelayRow) mutationBuilder.createRelayRowById(model, id) else DBIO.successful(())
    } yield CreateNodeResult(id, mutaction)
  }

  private def createNodeAndConnectToParent(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext) = {
    relation.manifestation match {
      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == model.name =>
        val inlineField  = relation.getFieldOnModel(model.name)
        val argsMap      = mutaction.nonListArgs.raw.asRoot.map
        val modifiedArgs = argsMap.updated(inlineField.name, parentId)
        mutationBuilder.createDataItem(model, PrismaArgs(RootGCValue(modifiedArgs)))
      case _ =>
        for {
          id <- mutationBuilder.createDataItem(model, mutaction.nonListArgs)
          _  <- mutationBuilder.createRelation(mutaction.relationField, parentId, id)
        } yield id

    }
  }

  override def requiredCheck(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] =
    mutaction.topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => noCheckRequired
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
        noCheckRequired
    }

  override def removalActions(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIO[Unit]] =
    mutaction.topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => requiredRelationViolation
          case (false, true, false, false)  => List(removalByParent(parentId))
          case (false, false, false, true)  => List(removalByParent(parentId))
          case (false, false, false, false) => List(removalByParent(parentId))
          case (true, false, false, true)   => noActionRequired
          case (true, false, false, false)  => noActionRequired
          case (false, true, true, false)   => List(removalByParent(parentId))
          case (false, false, true, false)  => List(removalByParent(parentId))
          case (true, false, true, false)   => noActionRequired
          case _                            => sysError
        }

      case true =>
        noActionRequired
    }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

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

trait CascadingDeleteSharedStuff extends DatabaseMutactionInterpreter {
  def schema: Schema
  implicit def ec: ExecutionContext

  def performCascadingDelete(mutationBuilder: PostgresApiDatabaseMutationBuilder, model: Model, parentId: IdGCValue): DBIO[Unit] = {
    val actions = model.cascadingRelationFields.map { field =>
      recurse(
        mutationBuilder = mutationBuilder,
        parentField = field,
        parentIds = Vector(parentId),
        visitedModels = Vector(model)
      )
    }
    DBIO.seq(actions: _*)
  }

  private def recurse(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentField: RelationField,
      parentIds: Vector[IdGCValue],
      visitedModels: Vector[Model]
  ): DBIO[Unit] = {
    for {
      ids            <- mutationBuilder.queryIdsByParentIds(parentField, parentIds)
      model          = parentField.relatedModel_!
      _              = if (visitedModels.contains(model)) throw APIErrors.CascadingDeletePathLoops()
      nextCascadings = model.cascadingRelationFields.filter(_ != parentField)
      childActions   = nextCascadings.map(field => recurse(mutationBuilder, field, ids, visitedModels :+ model))
      _              <- DBIO.seq(childActions: _*)
      // eigentliche Actions
      _ <- checkTheseOnes(mutationBuilder, parentField, ids)
      _ <- mutationBuilder.deleteNodes(model, ids)
    } yield ()
  }

  private def checkTheseOnes(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentField: RelationField, parentIds: Vector[IdGCValue]) = {
    val model                          = parentField.relatedModel_!
    val fieldsWhereThisModelIsRequired = schema.fieldsWhereThisModelIsRequired(model).filter(_ != parentField)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(parentIds, field))
    DBIO.sequence(actions)
  }
  /**
  * input: relationField, parentIds
  * 1. recurse for children
  * 2. check for required relation violations
  * 3. delete itself
  */
}

case class DeleteDataItemInterpreter(mutaction: TopLevelDeleteNode)(implicit val ec: ExecutionContext)
    extends DatabaseMutactionInterpreter
    with CascadingDeleteSharedStuff {

  override def schema = mutaction.where.model.schema

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      nodeOpt <- mutationBuilder.queryNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) =>
                 for {
                   _ <- performCascadingDelete(mutationBuilder, mutaction.where.model, node.id)
                   _ <- checkForRequiredRelationsViolations(mutationBuilder, node.id)
                   _ <- mutationBuilder.deleteNodeById(mutaction.where.model, node.id)
                 } yield node
               case None =>
                 DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
             }
    } yield DeleteNodeResult(node.id, node, mutaction)
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: PostgresApiDatabaseMutationBuilder, id: IdGCValue): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = schema.fieldsWhereThisModelIsRequired(mutaction.where.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(id, field))
    DBIO.sequence(actions)
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

case class DeleteDataItemNestedInterpreter(mutaction: NestedDeleteNode)(implicit val ec: ExecutionContext)
    extends DatabaseMutactionInterpreter
    with CascadingDeleteSharedStuff {

  override def schema = mutaction.project.schema
  val parentField     = mutaction.relationField
  val parent          = mutaction.relationField.model
  val child           = mutaction.relationField.relatedModel_!

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      childId <- getChildId(mutationBuilder, parentId)
      _       <- mutationBuilder.ensureThatParentIsConnected(parentField, parentId)
      _       <- performCascadingDelete(mutationBuilder, child, childId)
      _       <- checkForRequiredRelationsViolations(mutationBuilder, childId)
      _       <- mutationBuilder.deleteNodeById(child, childId)
    } yield UnitDatabaseMutactionResult
  }

  private def getChildId(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue): DBIO[IdGCValue] = {
    mutaction.where match {
      case Some(where) =>
        mutationBuilder.queryIdFromWhere(where).map {
          case Some(id) => id
          case None     => throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        mutationBuilder.queryIdByParentId(parentField, parentId).map {
          case Some(id) => id
          case None =>
            throw NodesNotConnectedError(
              relation = parentField.relation,
              parent = parentField.model,
              parentWhere = Some(NodeSelector.forIdGCValue(parent, parentId)),
              child = parentField.relatedModel_!,
              childWhere = None
            )
        }
    }
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.relationField.relatedModel_!)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(parentId, field))
    DBIO.sequence(actions)
  }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

//Fixme also switch this to fetch the Ids first
case class DeleteDataItemsInterpreter(mutaction: DeleteNodes)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) =
    for {
      _   <- checkForRequiredRelationsViolations(mutationBuilder)
      ids <- mutationBuilder.queryIdsByWhereFilter(mutaction.model, mutaction.whereFilter)
      _   <- mutationBuilder.deleteNodes(mutaction.model, ids)
    } yield UnitDatabaseMutactionResult

  private def checkForRequiredRelationsViolations(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[_] = {
    val model                          = mutaction.model
    val filter                         = mutaction.whereFilter
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByFieldAndFilter(model, filter, field))
    DBIO.sequence(actions)
  }
}

case class ResetDataInterpreter(mutaction: ResetData) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    mutationBuilder.truncateTables(mutaction.project)
  }
}

case class UpdateDataItemInterpreter(mutaction: TopLevelUpdateNode) extends DatabaseMutactionInterpreter with SharedUpdateLogic {
  val model             = mutaction.where.model
  val nonListArgs       = mutaction.nonListArgs
  override def listArgs = mutaction.listArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parent: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      nodeOpt <- mutationBuilder.queryNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) => doIt(mutationBuilder, node.id).andThen(DBIO.successful(node))
               case None       => DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
             }
    } yield UpdateNodeResult(node.id, node, mutaction)
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

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

case class NestedUpdateDataItemInterpreter(mutaction: NestedUpdateNode) extends DatabaseMutactionInterpreter with SharedUpdateLogic {
  val model       = mutaction.relationField.relatedModel_!
  val parent      = mutaction.relationField.model
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _ <- verifyWhere(mutationBuilder, mutaction.where)
      idOpt <- mutaction.where match {
                case Some(where) => mutationBuilder.queryIdByParentIdAndWhere(mutaction.relationField, parentId, where)
                case None        => mutationBuilder.queryIdByParentId(mutaction.relationField, parentId)
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
  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

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

  def verifyWhere(mutationBuilder: PostgresApiDatabaseMutationBuilder, where: Option[NodeSelector])(implicit ec: ExecutionContext) = {
    where match {
      case Some(where) =>
        for {
          id <- mutationBuilder.queryIdFromWhere(where)
        } yield {
          if (id.isEmpty) throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        DBIO.successful(())
    }
  }

  def doIt(mutationBuilder: PostgresApiDatabaseMutationBuilder, id: IdGCValue)(implicit ec: ExecutionContext): DBIO[IdGCValue] = {
    for {
      _ <- mutationBuilder.updateDataItemById(model, id, nonListArgs)
      _ <- mutationBuilder.setScalarListById(model, id, listArgs)
    } yield id
  }
}

case class UpdateDataItemsInterpreter(mutaction: UpdateNodes) extends DatabaseMutactionInterpreter {
  //update Lists before updating the nodes
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val nonListActions = mutationBuilder.updateDataItems(mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
    val listActions    = mutationBuilder.setManyScalarLists(mutaction.model, mutaction.listArgs, mutaction.whereFilter)
    DBIOAction.seq(listActions, nonListActions)
  }
}

case class UpsertDataItemInterpreter(mutaction: TopLevelUpsertNode) extends DatabaseMutactionInterpreter {
  val model   = mutaction.where.model
  val project = mutaction.project
//  val createArgs = mutaction.nonListCreateArgs
//  val updateArgs = mutaction.nonListUpdateArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
//    val createNested: Vector[DBIOAction[Any, NoStream, Effect.All]] =
//      mutaction.createMutactions.map(executor.interpreterFor).map(_.newActionWithErrorMapped(mutationBuilder, parentId))
//    val updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]] =
//      mutaction.updateMutactions.map(executor.interpreterFor).map(_.newActionWithErrorMapped(mutationBuilder, parentId))

//    val createAction = mutationBuilder.setScalarList(mutaction.createPath.lastCreateWhere_!, mutaction.listCreateArgs)
//    val updateAction = mutationBuilder.setScalarList(mutaction.updatePath.lastCreateWhere_!, mutaction.listUpdateArgs)
//    mutationBuilder
//      .upsert(
//        createPath = mutaction.createPath,
//        updatePath = mutaction.updatePath,
//        createArgs = createArgs,
//        updateArgs = updateArgs,
//        create = createAction,
//        update = updateAction,
//        createNested = createNested,
//        updateNested = updateNested
//      )
//      .andThen(unitResult)

    for {
      id <- mutationBuilder.queryIdFromWhere(mutaction.where)
    } yield
      id match {
        case Some(_) => UpsertNodeResult(mutaction.update, mutaction)
        case None    => UpsertNodeResult(mutaction.create, mutaction)
      }
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

  val upsertErrors: PartialFunction[Throwable, UserFacingError] = {
    case e: PSQLException if e.getSQLState == "23505" && getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, getFieldOption(model, e).get)

    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("") //todo

    case e: PSQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull(e.getMessage)
  }

//  val createErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.createMutactions.map(executor.interpreterFor).map(_.errorMapper)
//  val updateErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.updateMutactions.map(executor.interpreterFor).map(_.errorMapper)
//
//  override val errorMapper = (updateErrors ++ createErrors).foldLeft(upsertErrors)(_ orElse _)

}

case class NestedUpsertDataItemInterpreter(mutaction: NestedUpsertNode) extends DatabaseMutactionInterpreter {
  val model = mutaction.relationField.relatedModel_!

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      id <- mutaction.where match {
             case Some(where) => mutationBuilder.queryIdByParentIdAndWhere(mutaction.relationField, parentId, where)
             case None        => mutationBuilder.queryIdByParentId(mutaction.relationField, parentId)
           }
    } yield
      id match {
        case Some(_) => UpsertNodeResult(mutaction.update, mutaction)
        case None    => UpsertNodeResult(mutaction.create, mutaction)
      }
  }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

//case class UpsertDataItemIfInRelationWithInterpreter(mutaction: UpsertDataItemIfInRelationWith, executor: PostgresDatabaseMutactionExecutor)
//    extends DatabaseMutactionInterpreter {
//  val project         = mutaction.project
//  val model           = mutaction.createPath.lastModel
//  val relationChecker = NestedCreateRelationInterpreter(NestedCreateRelation(project, mutaction.createPath, false))
//
//  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
//
//    val createNested: Vector[DBIOAction[Any, NoStream, Effect.All]] = mutaction.createMutactions.map(executor.interpreterFor).map(_.action(mutationBuilder))
//    val updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]] = mutaction.updateMutactions.map(executor.interpreterFor).map(_.action(mutationBuilder))
//
//    val createCheck       = DBIOAction.seq(relationChecker.allActions(mutationBuilder): _*)
//    val scalarListsCreate = mutationBuilder.setScalarList(mutaction.createPath, mutaction.createListArgs)
//    val scalarListsUpdate = mutationBuilder.setScalarList(mutaction.updatePath, mutaction.updateListArgs)
//    mutationBuilder.upsertIfInRelationWith(
//      createPath = mutaction.createPath,
//      updatePath = mutaction.updatePath,
//      createArgs = mutaction.createNonListArgs,
//      updateArgs = mutaction.updateNonListArgs,
//      scalarListCreate = scalarListsCreate,
//      scalarListUpdate = scalarListsUpdate,
//      createCheck = createCheck,
//      createNested,
//      updateNested
//    )
//  }
//
//  val upsertErrors: PartialFunction[Throwable, UserFacingError] = {
//    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
//    case e: PSQLException if e.getSQLState == "23505" && getFieldOption(model, e).isDefined =>
//      APIErrors.UniqueConstraintViolation(mutaction.createPath.lastModel.name, getFieldOption(model, e).get)
//
//    case e: PSQLException if e.getSQLState == "23503" =>
//      APIErrors.NodeDoesNotExist("") //todo
//
//    case e: PSQLException if e.getSQLState == "23502" =>
//      APIErrors.FieldCannotBeNull()
//
//    case e: PSQLException if relationChecker.causedByThisMutaction(e.getMessage) =>
//      throw RequiredRelationWouldBeViolated(project, mutaction.createPath.lastRelation_!)
//  }
//
//  val createErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.createMutactions.map(executor.interpreterFor).map(_.errorMapper)
//  val updateErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.updateMutactions.map(executor.interpreterFor).map(_.errorMapper)
//  override val errorMapper                                              = (updateErrors ++ createErrors).foldLeft(upsertErrors)(_ orElse _)
//}

case class CreateDataItemsImportInterpreter(mutaction: ImportNodes) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[Vector[String]] = mutationBuilder.createDataItemsImport(mutaction)
}

case class CreateRelationRowsImportInterpreter(mutaction: ImportRelations) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[Vector[String]] = mutationBuilder.createRelationRowsImport(mutaction)
}

case class PushScalarListsImportInterpreter(mutaction: ImportScalarLists)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[Vector[String]] = mutationBuilder.pushScalarListsImport(mutaction)
}
