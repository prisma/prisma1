package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.postgresql.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.{APIErrors, UserFacingError}
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models._
import org.postgresql.util.PSQLException
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

case class AddDataItemToManyRelationByPathInterpreter(mutaction: AddDataItemToManyRelationByPath) extends DatabaseMutactionInterpreter {

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ??? //Fixme remove this alltogether
}

case class CascadingDeleteRelationMutactionsInterpreter(mutaction: CascadingDeleteRelationMutactions) extends DatabaseMutactionInterpreter {
  val path    = mutaction.path
  val project = mutaction.project
  val schema  = project.schema

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  val otherFieldsWhereThisModelIsRequired = path.lastEdge match {
    case Some(edge) => fieldsWhereThisModelIsRequired.filter(f => f != edge.parentField)
    case None       => fieldsWhereThisModelIsRequired
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val requiredCheck = otherFieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByFieldLegacy(path, field, causeString(field)))
    val deleteAction  = List(mutationBuilder.cascadingDeleteChildActions(path))
    val allActions    = requiredCheck ++ deleteAction
    DBIOAction.seq(allActions: _*)
  }

  override def errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    otherFieldsWhereThisModelIsRequired.collectFirst { case f if cause.contains(causeString(f)) => f.relation }

  private def causeString(field: RelationField) = path.lastEdge match {
    case Some(edge: NodeEdge) =>
      s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.relationTableName}@${field.oppositeRelationSide}@${edge.childWhere.value}-"
    case _ => s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.relationTableName}@${field.oppositeRelationSide}-"
  }
}

case class CreateDataItemInterpreter(mutaction: CreateDataItem, includeRelayRow: Boolean = true) extends DatabaseMutactionInterpreter {
  val model = mutaction.model

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(
      implicit ec: ExecutionContext): DBIO[DatabaseMutactionResult] = {
    for {
      createResult <- mutationBuilder.createDataItem(model, mutaction.nonListArgs)
      _            <- mutationBuilder.setScalarList(NodeSelector.forIdGCValue(model, createResult.createdId), mutaction.listArgs)
      _            <- if (includeRelayRow) mutationBuilder.createRelayRow(NodeSelector.forIdGCValue(model, createResult.createdId)) else DBIO.successful(())
    } yield createResult
  }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.model, e).get)
    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeDoesNotExist("")
  }

}

case class NestedCreateDataItemInterpreter(mutaction: NestedCreateDataItem, includeRelayRow: Boolean = true)(implicit val ec: ExecutionContext)
    extends DatabaseMutactionInterpreter
    with NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField
  val project                = mutaction.project
  val model                  = relationField.relatedModel_!
  val parent                 = relationField.model

  override def addAction(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder) = ???

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _            <- DBIO.sequence(requiredCheck(parentId)(mutationBuilder))
      _            <- DBIO.sequence(removalActions(parentId)(mutationBuilder))
      createResult <- createNodeAndConnectToParent(mutationBuilder, parentId)
      _            <- mutationBuilder.setScalarList(NodeSelector.forIdGCValue(model, createResult.createdId), mutaction.listArgs)
      _            <- if (includeRelayRow) mutationBuilder.createRelayRow(NodeSelector.forIdGCValue(model, createResult.createdId)) else DBIO.successful(())
    } yield createResult
  }

  private def createNodeAndConnectToParent(
      mutationBuilder: PostgresApiDatabaseMutationBuilder,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext) = {

    if (relation.isInlineRelation) {
      val inlineField  = relation.getFieldOnModel(model.name)
      val argsMap      = mutaction.nonListArgs.raw.asRoot.map
      val modifiedArgs = argsMap.updated(inlineField.name, parentId)
      mutationBuilder.createDataItem(model, PrismaArgs(RootGCValue(modifiedArgs)))
    } else {
      for {
        createResult <- mutationBuilder.createDataItem(model, mutaction.nonListArgs)
        _            <- mutationBuilder.createRelationRowByPath(mutaction.relationField, parentId, createResult.createdId)
      } yield createResult
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
  }
}

case class DeleteDataItemInterpreter(mutaction: DeleteDataItem)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _ <- mutationBuilder.deleteRelayRowByWhere(mutaction.where)
      _ <- mutationBuilder.deleteDataItemByWhere(mutaction.where)
    } yield UnitDatabaseMutactionResult
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

case class DeleteDataItemNestedInterpreter(mutaction: NestedDeleteDataItem)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  val parentField = mutaction.relationField
  val parent      = mutaction.relationField.model
  val child       = mutaction.relationField.relatedModel_!

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      childId <- getChildId(mutationBuilder, parentId)
      _       <- mutationBuilder.ensureThatParentIsConnected(parentField, parentId)
      _       <- checkForRequiredRelationsViolations(mutationBuilder, childId)
      _       <- mutationBuilder.deleteRelayRowByWhere(NodeSelector.forIdGCValue(child, childId))
      _       <- mutationBuilder.deleteDataItemByWhere(NodeSelector.forIdGCValue(child, childId))
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

case class DeleteDataItemsInterpreter(mutaction: DeleteDataItems) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = DBIOAction.seq(
    mutationBuilder.deleteRelayIds(mutaction.model, mutaction.whereFilter),
    mutationBuilder.deleteDataItems(mutaction.model, mutaction.whereFilter)
  )
}

case class DeleteManyRelationChecksInterpreter(mutaction: DeleteManyRelationChecks) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val model   = mutaction.model
  val filter  = mutaction.whereFilter
  val schema  = project.schema

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val requiredChecks =
      fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByFieldAndFilter(model, filter, field, causeString(field)))
    DBIOAction.seq(requiredChecks: _*)
  }

  override def errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = fieldsWhereThisModelIsRequired.collectFirst {
    case f if cause.contains(causeString(f)) => f.relation
  }

  private def causeString(field: RelationField) =
    s"-OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER@${field.relation.relationTableName}@${field.oppositeRelationSide}-"

}

case class DeleteRelationCheckInterpreter(mutaction: DeleteRelationCheck) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path
  val schema  = project.schema

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val requiredCheck = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByFieldLegacy(path, field, causeString(field)))
    DBIOAction.seq(requiredCheck: _*)
  }

  override val errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    fieldsWhereThisModelIsRequired.collectFirst { case f if cause.contains(causeString(f)) => f.relation }

  private def causeString(field: RelationField) = path.lastEdge match {
    case Some(edge: NodeEdge) =>
      s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.relationTableName}@${field.oppositeRelationSide}@${edge.childWhere.value}-"
    case _ => s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.relationTableName}@${field.oppositeRelationSide}-"
  }
}

case class ResetDataInterpreter(mutaction: ResetDataMutaction) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val truncateTables = DBIOAction.sequence(mutaction.tableNames.map(mutationBuilder.truncateTable))
    DBIOAction.seq(truncateTables)
  }
}

case class UpdateDataItemInterpreter(mutaction: UpdateDataItem) extends DatabaseMutactionInterpreter with SharedUpdateLogic {
  val model       = mutaction.where.model
  val nonListArgs = mutaction.nonListArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parent: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      id <- mutationBuilder.queryIdFromWhere(mutaction.where)
      _ <- id match {
            case Some(id) => doIt(mutationBuilder, id)
            case None     => DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
          }
    } yield UpdateItemResult(id)
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
  }
}

case class NestedUpdateDataItemInterpreter(mutaction: NestedUpdateDataItem) extends DatabaseMutactionInterpreter with SharedUpdateLogic {
  val model       = mutaction.relationField.relatedModel_!
  val parent      = mutaction.relationField.model
  val nonListArgs = mutaction.nonListArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _ <- verifyWhere(mutationBuilder, mutaction.where)
      id <- mutaction.where match {
             case Some(where) => mutationBuilder.queryIdByParentIdAndWhere(mutaction.relationField, parentId, where)
             case None        => mutationBuilder.queryIdByParentId(mutaction.relationField, parentId)
           }
      _ <- id match {
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
    } yield UpdateItemResult(id)
  }
  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(model, e).get)

    case e: PSQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull()
  }
}

trait SharedUpdateLogic {
  def model: Model
  def nonListArgs: PrismaArgs

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

  def doIt(mutationBuilder: PostgresApiDatabaseMutationBuilder, id: IdGCValue)(implicit ec: ExecutionContext): DBIO[Unit] = {
    for {
      _ <- mutationBuilder.updateDataItemById(model, id, nonListArgs)
      //                _ <- mutationBuilder.setScalarList(path.lastCreateWhere_!, listArgs)
    } yield ()
  }
}

case class UpdateDataItemsInterpreter(mutaction: UpdateDataItems) extends DatabaseMutactionInterpreter {
  //update Lists before updating the nodes
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val nonListActions = mutationBuilder.updateDataItems(mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
    val listActions    = mutationBuilder.setManyScalarLists(mutaction.model, mutaction.listArgs, mutaction.whereFilter)
    DBIOAction.seq(listActions, nonListActions)
  }
}

case class UpsertDataItemInterpreter(mutaction: UpsertDataItem, executor: PostgresDatabaseMutactionExecutor) extends DatabaseMutactionInterpreter {
  val model      = mutaction.where.model
  val project    = mutaction.project
  val createArgs = mutaction.nonListCreateArgs
  val updateArgs = mutaction.nonListUpdateArgs

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    val createNested: Vector[DBIOAction[Any, NoStream, Effect.All]] =
      mutaction.createMutactions.map(executor.interpreterFor).map(_.newActionWithErrorMapped(mutationBuilder, parentId))
    val updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]] =
      mutaction.updateMutactions.map(executor.interpreterFor).map(_.newActionWithErrorMapped(mutationBuilder, parentId))

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
      result <- id match {
                 case Some(id) => mutationBuilder.updateDataItemById(model, id, updateArgs)
                 case None     => mutationBuilder.createDataItem(model, createArgs)
               }
    } yield result
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

  val createErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.createMutactions.map(executor.interpreterFor).map(_.errorMapper)
  val updateErrors: Vector[PartialFunction[Throwable, UserFacingError]] = mutaction.updateMutactions.map(executor.interpreterFor).map(_.errorMapper)

  override val errorMapper = (updateErrors ++ createErrors).foldLeft(upsertErrors)(_ orElse _)

}

case class NestedUpsertDataItemInterpreter(mutaction: NestedUpsertDataItem) extends DatabaseMutactionInterpreter {

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parent: IdGCValue)(implicit ec: ExecutionContext) = ???
  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder)                                                      = ???
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

case class VerifyConnectionInterpreter(mutaction: VerifyConnection) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path
  val schema  = project.schema

  val causeString = path.lastEdge_! match {
    case _: ModelEdge =>
      s"CONNECTIONFAILURETRIGGERPATH@${path.lastRelation_!.relationTableName}@${path.columnForParentSideOfLastEdge}"
    case edge: NodeEdge =>
      s"CONNECTIONFAILURETRIGGERPATH@${path.lastRelation_!.relationTableName}@${path.columnForParentSideOfLastEdge}@${path.columnForChildSideOfLastEdge}@${edge.childWhere.value}}"
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.connectionFailureTrigger(path, causeString)

  override val errorMapper = { case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodesNotConnectedErrorByPath(path) }
}

case class VerifyWhereInterpreter(mutaction: VerifyWhere) extends DatabaseMutactionInterpreter {
  val project     = mutaction.project
  val where       = mutaction.where
  val causeString = s"WHEREFAILURETRIGGER@${where.model.name}@${where.field.name}@${where.value}"

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.whereFailureTrigger(where, causeString)

  override val errorMapper = { case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodeNotFoundForWhereError(where) }
}

case class CreateDataItemsImportInterpreter(mutaction: CreateDataItemsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.createDataItemsImport(mutaction)
}

case class CreateRelationRowsImportInterpreter(mutaction: CreateRelationRowsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.createRelationRowsImport(mutaction)
}

case class PushScalarListsImportInterpreter(mutaction: PushScalarListsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.pushScalarListsImport(mutaction)
}
