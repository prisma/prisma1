package com.prisma.api.connector.mysql.impl

import java.sql.{SQLException, SQLIntegrityConstraintViolationException}
import com.prisma.api.connector._
import com.prisma.api.connector.mysql.DatabaseMutactionInterpreter
import com.prisma.api.connector.mysql.database.DatabaseMutationBuilder
import com.prisma.api.connector.mysql.database.DatabaseMutationBuilder.{
  cascadingDeleteChildActions,
  oldParentFailureTriggerByField,
  oldParentFailureTriggerByFieldAndFilter
}
import com.prisma.api.connector.mysql.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterString
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._

case class AddDataItemToManyRelationByPathInterpreter(mutaction: AddDataItemToManyRelationByPath) extends DatabaseMutactionInterpreter {

  override val action = DatabaseMutationBuilder.createRelationRowByPath(mutaction.project.id, mutaction.path)
}

case class CascadingDeleteRelationMutactionsInterpreter(mutaction: CascadingDeleteRelationMutactions) extends DatabaseMutactionInterpreter {
  val path    = mutaction.path
  val project = mutaction.project

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  val otherFieldsWhereThisModelIsRequired = path.lastEdge match {
    case Some(edge) => fieldsWhereThisModelIsRequired.filter(f => f != edge.parentField)
    case None       => fieldsWhereThisModelIsRequired
  }

  override val action = {
    val requiredCheck = otherFieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByField(project, path, _))
    val deleteAction  = List(cascadingDeleteChildActions(project.id, path))
    val allActions    = requiredCheck ++ deleteAction
    DBIOAction.seq(allActions: _*)
  }

  override def errorMapper = {
    case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    otherFieldsWhereThisModelIsRequired.collectFirst { case f if causedByThisMutactionChildOnly(f, cause) => f.relation.get }

  private def causedByThisMutactionChildOnly(field: Field, cause: String) = {
    val parentCheckString = s"`${field.relation.get.relationTableName}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}

case class CreateDataItemInterpreter(mutaction: CreateDataItem) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  override val action = {
    val createNonList  = DatabaseMutationBuilder.createDataItem(project.id, path, mutaction.nonListArgs)
    val createRelayRow = DatabaseMutationBuilder.createRelayRow(project.id, path)
    val listAction     = DatabaseMutationBuilder.getDbActionForScalarLists(project, path, mutaction.listArgs)

    DBIO.seq(createNonList, createRelayRow, listAction)
  }

  override val errorMapper = {
    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).get)
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("")
  }
}

case class DeleteDataItemInterpreter(mutaction: DeleteDataItem) extends DatabaseMutactionInterpreter {
  override val action = DBIO.seq(
    DatabaseMutationBuilder.deleteRelayRow(mutaction.project.id, mutaction.path),
    DatabaseMutationBuilder.deleteDataItem(mutaction.project.id, mutaction.path)
  )
}

case class DeleteDataItemNestedInterpreter(mutaction: DeleteDataItemNested) extends DatabaseMutactionInterpreter {
  override val action = DBIO.seq(
    DatabaseMutationBuilder.deleteRelayRow(mutaction.project.id, mutaction.path),
    DatabaseMutationBuilder.deleteDataItem(mutaction.project.id, mutaction.path)
  )
}

case class DeleteDataItemsInterpreter(mutaction: DeleteDataItems) extends DatabaseMutactionInterpreter {
  val project     = mutaction.project
  val model       = mutaction.model
  val whereFilter = mutaction.whereFilter

  override val action = DBIOAction.seq(
    DatabaseMutationBuilder.deleteRelayIds(project, model, whereFilter),
    DatabaseMutationBuilder.deleteDataItems(project, model, whereFilter)
  )
}

case class DeleteManyRelationChecksInterpreter(mutaction: DeleteManyRelationChecks) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val model   = mutaction.model
  val filter  = mutaction.whereFilter

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)

  override val action = {
    val requiredChecks = fieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByFieldAndFilter(project, model, filter, _))
    DBIOAction.seq(requiredChecks: _*)
  }

  override def errorMapper = {
    case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = fieldsWhereThisModelIsRequired.collectFirst {
    case f if causedByThisMutactionChildOnly(f, cause) => f.relation.get
  }

  private def causedByThisMutactionChildOnly(field: Field, cause: String) = {
    val parentCheckString = s"`${field.relation.get.relationTableName}` OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER WHERE `${field.oppositeRelationSide.get}`"
    cause.contains(parentCheckString) //todo add filter
  }
}

case class DeleteRelationCheckInterpreter(mutaction: DeleteRelationCheck) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  override val action = {
    val requiredCheck = fieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByField(project, path, _))
    DBIOAction.seq(requiredCheck: _*)
  }

  override val errorMapper = {
    case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    fieldsWhereThisModelIsRequired.collectFirst { case f if causedByThisMutactionChildOnly(f, cause) => f.relation.get }

  private def causedByThisMutactionChildOnly(field: Field, cause: String) = {
    val parentCheckString = s"`${field.relation.get.relationTableName}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}

case class ResetDataInterpreter(mutaction: ResetDataMutaction) extends DatabaseMutactionInterpreter {
  val disableConstraints = DatabaseMutationBuilder.disableForeignKeyConstraintChecks
  val truncateTables     = DBIOAction.seq(mutaction.tableNames.map(DatabaseMutationBuilder.resetData(mutaction.project.id, _)): _*)
  val enableConstraints  = DatabaseMutationBuilder.enableForeignKeyConstraintChecks

  override val action = DBIOAction.seq(disableConstraints, truncateTables, enableConstraints)
}

case class UpdateDataItemInterpreter(mutaction: UpdateDataItem) extends DatabaseMutactionInterpreter {
  val project       = mutaction.project
  val path          = mutaction.path
  val nonListAction = DatabaseMutationBuilder.updateDataItemByPath(project.id, path, mutaction.nonListArgs)
  val listAction    = DatabaseMutationBuilder.getDbActionForScalarLists(project, path, mutaction.listArgs)

  override val action = DBIO.seq(listAction, nonListAction)

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeNotFoundForWhereError(path.root)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()
  }
}

case class NestedUpdateDataItemInterpreter(mutaction: NestedUpdateDataItem) extends DatabaseMutactionInterpreter { //todo kick this out
  val project       = mutaction.project
  val path          = mutaction.path
  val args          = mutaction.args
  val nonListAction = DatabaseMutationBuilder.updateDataItemByPath(project.id, path, args)
  val listAction    = DatabaseMutationBuilder.getDbActionForScalarLists(project, path, mutaction.listArgs)

  override val action = DBIO.seq(listAction, nonListAction)

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOption(args.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(args.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeNotFoundForWhereError(path.root) // todo check this

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()
  }
}

case class UpdateDataItemsInterpreter(mutaction: UpdateDataItems) extends DatabaseMutactionInterpreter {
  override val action = DatabaseMutationBuilder.updateDataItems(mutaction.project.id, mutaction.model, mutaction.updateArgs, mutaction.where)
}

case class UpsertDataItemInterpreter(mutaction: UpsertDataItem) extends DatabaseMutactionInterpreter {
  val model      = mutaction.updatePath.lastModel
  val project    = mutaction.project
  val createArgs = mutaction.nonListCreateArgs
  val updateArgs = mutaction.nonListUpdateArgs

  def updatedRoot(path: Path, args: PrismaArgs): Path = {
    val whereFieldValue = args.getFieldValue(path.root.field.name)
    val updatedWhere    = whereFieldValue.map(path.root.updateValue).getOrElse(path.root)
    path.copy(root = updatedWhere)
  }

  override val action = {
    val createAction = DatabaseMutationBuilder.getDbActionForScalarLists(project, mutaction.createPath, mutaction.listCreateArgs)
    val updateAction = DatabaseMutationBuilder.getDbActionForScalarLists(project, mutaction.updatePath, mutaction.listUpdateArgs)
    DatabaseMutationBuilder.upsert(project.id, mutaction.createPath, mutaction.updatePath, createArgs, updateArgs, createAction, updateAction)
  }

  override val errorMapper = {
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && getFieldOption(createArgs.keys ++ updateArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, getFieldOption(createArgs.keys ++ updateArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("") //todo

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull(e.getCause.getMessage)
  }
}

case class UpsertDataItemIfInRelationWithInterpreter(mutaction: UpsertDataItemIfInRelationWith) extends DatabaseMutactionInterpreter {
  val project = mutaction.project

  val scalarListsCreate = DatabaseMutationBuilder.getDbActionForScalarLists(project, mutaction.createPath, mutaction.createListArgs)
  val scalarListsUpdate = DatabaseMutationBuilder.getDbActionForScalarLists(project, mutaction.updatePath, mutaction.updateListArgs)
  val relationChecker   = NestedCreateRelationInterpreter(NestedCreateRelation(project, mutaction.createPath, false))
  val createCheck       = DBIOAction.seq(relationChecker.allActions: _*)

  override val action = DatabaseMutationBuilder.upsertIfInRelationWith(
    project = project,
    createPath = mutaction.createPath,
    updatePath = mutaction.updatePath,
    createArgs = mutaction.createNonListArgs,
    updateArgs = mutaction.updateNonListArgs,
    scalarListCreate = scalarListsCreate,
    scalarListUpdate = scalarListsUpdate,
    createCheck = createCheck
  )

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: SQLIntegrityConstraintViolationException
        if e.getErrorCode == 1062 && getFieldOption(mutaction.createNonListArgs.keys ++ mutaction.updateNonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.createPath.lastModel.name,
                                          getFieldOption(mutaction.createNonListArgs.keys ++ mutaction.updateNonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("") //todo

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()

    case e: SQLException if e.getErrorCode == 1242 && relationChecker.causedByThisMutaction(mutaction.createPath, e.getCause.toString) =>
      throw RequiredRelationWouldBeViolated(project, mutaction.createPath.lastRelation_!)
  }
}

case class VerifyConnectionInterpreter(mutaction: VerifyConnection) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  override val action = DatabaseMutationBuilder.connectionFailureTrigger(project, path)

  override val errorMapper = {
    case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(path)
  }

  private def causedByThisMutaction(cause: String) = {
    val string = s"`${path.lastRelation_!.relationTableName}` CONNECTIONFAILURETRIGGERPATH WHERE "

    path.lastEdge_! match {
      case _: ModelEdge   => cause.contains(string ++ s" `${path.parentSideOfLastEdge}`")
      case edge: NodeEdge => cause.contains(string ++ s" `${path.childSideOfLastEdge}`") && cause.contains(parameterString(edge.childWhere))
    }
  }
}

case class VerifyWhereInterpreter(mutaction: VerifyWhere) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val where   = mutaction.where

  override val action = DatabaseMutationBuilder.whereFailureTrigger(project, where)

  override val errorMapper = {
    case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodeNotFoundForWhereError(where)
  }

  private def causedByThisMutaction(cause: String) = {
    val modelString = s"`${where.model.name}` WHEREFAILURETRIGGER WHERE `${where.field.name}`"
    cause.contains(modelString) && cause.contains(parameterString(where))
  }
}

case class CreateDataItemsImportInterpreter(mutaction: CreateDataItemsImport) extends DatabaseMutactionInterpreter {
  override val action = DatabaseMutationBuilder.createDataItemsImport(mutaction)
}

case class CreateRelationRowsImportInterpreter(mutaction: CreateRelationRowsImport) extends DatabaseMutactionInterpreter {
  override val action = DatabaseMutationBuilder.createRelationRowsImport(mutaction)
}

case class PushScalarListsImportInterpreter(mutaction: PushScalarListsImport) extends DatabaseMutactionInterpreter {
  override val action = DatabaseMutationBuilder.pushScalarListsImport(mutaction)
}
