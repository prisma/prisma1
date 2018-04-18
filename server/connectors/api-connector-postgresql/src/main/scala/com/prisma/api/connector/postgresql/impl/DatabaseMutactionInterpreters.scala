package com.prisma.api.connector.postgresql.impl

import java.sql.{SQLException, SQLIntegrityConstraintViolationException}
import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.DatabaseMutationBuilder
import com.prisma.api.connector.postgresql.database.DatabaseMutationBuilder.{
  cascadingDeleteChildActions,
  oldParentFailureTriggerByField,
  oldParentFailureTriggerByFieldAndFilter
}
import com.prisma.api.connector.postgresql.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Relation}
import org.postgresql.util.PSQLException
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
    val requiredCheck = otherFieldsWhereThisModelIsRequired.map(field => oldParentFailureTriggerByField(project, path, field, causeString(field)))
    val deleteAction  = List(cascadingDeleteChildActions(project.id, path))
    val allActions    = requiredCheck ++ deleteAction
    DBIOAction.seq(allActions: _*)
  }

  override def errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    otherFieldsWhereThisModelIsRequired.collectFirst { case f if cause.contains(causeString(f)) => f.relation.get }

  private def causeString(field: Field) = path.lastEdge match {
    case Some(edge: NodeEdge) =>
      s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.get.relationTableName}@${field.oppositeRelationSide.get}@${edge.childWhere.fieldValueAsString}-"
    case _ => s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.get.relationTableName}@${field.oppositeRelationSide.get}-"
  }
}

case class CreateDataItemInterpreter(mutaction: CreateDataItem) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  override val action = {
    val createNonList  = DatabaseMutationBuilder.createDataItem(project.id, path, mutaction.nonListArgs)
    val createRelayRow = DatabaseMutationBuilder.createRelayRow(project.id, path)
    val listAction     = DatabaseMutationBuilder.setScalarList(project.id, path, mutaction.listArgs)

    DBIO.seq(createNonList, createRelayRow, listAction)
  }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).isDefined =>
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
  override val action = DBIOAction.seq(
    DatabaseMutationBuilder.deleteRelayIds(mutaction.project, mutaction.model, mutaction.whereFilter),
    DatabaseMutationBuilder.deleteDataItems(mutaction.project, mutaction.model, mutaction.whereFilter)
  )
}

case class DeleteManyRelationChecksInterpreter(mutaction: DeleteManyRelationChecks) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val model   = mutaction.model
  val filter  = mutaction.whereFilter

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)

  override val action = {
    val requiredChecks = fieldsWhereThisModelIsRequired.map(field => oldParentFailureTriggerByFieldAndFilter(project, model, filter, field, causeString(field)))
    DBIOAction.seq(requiredChecks: _*)
  }

  override def errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = fieldsWhereThisModelIsRequired.collectFirst {
    case f if cause.contains(causeString(f)) => f.relation.get
  }

  private def causeString(field: Field) =
    s"-OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER@${field.relation.get.relationTableName}@${field.oppositeRelationSide.get}-"

}

case class DeleteRelationCheckInterpreter(mutaction: DeleteRelationCheck) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  override val action = {
    val requiredCheck = fieldsWhereThisModelIsRequired.map(field => oldParentFailureTriggerByField(project, path, field, causeString(field)))
    DBIOAction.seq(requiredCheck: _*)
  }

  override val errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getMessage).get)
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    fieldsWhereThisModelIsRequired.collectFirst { case f if cause.contains(causeString(f)) => f.relation.get }

  private def causeString(field: Field) = path.lastEdge match {
    case Some(edge: NodeEdge) =>
      s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.get.relationTableName}@${field.oppositeRelationSide.get}@${edge.childWhere.fieldValueAsString}-"
    case _ => s"-OLDPARENTPATHFAILURETRIGGERBYFIELD@${field.relation.get.relationTableName}@${field.oppositeRelationSide.get}-"
  }
}

case class ResetDataInterpreter(mutaction: ResetDataMutaction) extends DatabaseMutactionInterpreter {
  val disableConstraints = DatabaseMutationBuilder.disableForeignKeyConstraintChecks
  val truncateTables     = DBIOAction.seq(mutaction.tableNames.map(DatabaseMutationBuilder.resetData(mutaction.project.id, _)): _*)
  val enableConstraints  = DatabaseMutationBuilder.enableForeignKeyConstraintChecks

  override val action = DBIOAction.seq(disableConstraints, truncateTables, enableConstraints)
}

case class UpdateDataItemInterpreter(mutaction: UpdateWrapper) extends DatabaseMutactionInterpreter {
  val (project, path, nonListArgs, listArgs) = mutaction match {
    case x: UpdateDataItem       => (x.project, x.path, x.nonListArgs, x.listArgs)
    case x: NestedUpdateDataItem => (x.project, x.path, x.nonListArgs, x.listArgs)
  }

  val nonListAction = DatabaseMutationBuilder.updateDataItemByPath(project.id, path, nonListArgs)
  val listAction    = DatabaseMutationBuilder.setScalarList(project.id, path, listArgs)

  override val action = DBIO.seq(listAction, nonListAction)

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(nonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeNotFoundForWhereError(path.root)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()
  }
}

case class UpdateDataItemsInterpreter(mutaction: UpdateDataItems) extends DatabaseMutactionInterpreter {
  val nonListActions = DatabaseMutationBuilder.updateDataItems(mutaction.project.id, mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
  val listActions    = DatabaseMutationBuilder.setManyScalarLists(mutaction.project.id, mutaction.model, mutaction.listArgs, mutaction.whereFilter)

  //update Lists before updating the nodes
  override val action = DBIOAction.seq(listActions, nonListActions)
}

case class UpsertDataItemInterpreter(mutaction: UpsertDataItem) extends DatabaseMutactionInterpreter {
  val model      = mutaction.updatePath.lastModel
  val project    = mutaction.project
  val createArgs = mutaction.nonListCreateArgs
  val updateArgs = mutaction.nonListUpdateArgs

  override val action = {
    val createAction = DatabaseMutationBuilder.setScalarList(project.id, mutaction.createPath, mutaction.listCreateArgs)
    val updateAction = DatabaseMutationBuilder.setScalarList(project.id, mutaction.updatePath, mutaction.listUpdateArgs)
    DatabaseMutationBuilder.upsert(project.id, mutaction.createPath, mutaction.updatePath, createArgs, updateArgs, createAction, updateAction)
  }

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && getFieldOption(createArgs.keys ++ updateArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, getFieldOption(createArgs.keys ++ updateArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("") //todo

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull(e.getCause.getMessage)
  }
}

case class UpsertDataItemIfInRelationWithInterpreter(mutaction: UpsertDataItemIfInRelationWith) extends DatabaseMutactionInterpreter {
  val project = mutaction.project

  val scalarListsCreate = DatabaseMutationBuilder.setScalarList(project.id, mutaction.createPath, mutaction.createListArgs)
  val scalarListsUpdate = DatabaseMutationBuilder.setScalarList(project.id, mutaction.updatePath, mutaction.updateListArgs)
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
    case e: PSQLException if e.getSQLState == "23505" && getFieldOption(mutaction.createNonListArgs.keys ++ mutaction.updateNonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.createPath.lastModel.name,
                                          getFieldOption(mutaction.createNonListArgs.keys ++ mutaction.updateNonListArgs.keys, e).get)

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("") //todo

    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
      APIErrors.FieldCannotBeNull()

    case e: PSQLException if relationChecker.causedByThisMutaction(e.getMessage) =>
      throw RequiredRelationWouldBeViolated(project, mutaction.createPath.lastRelation_!)
  }
}

case class VerifyConnectionInterpreter(mutaction: VerifyConnection) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path
  val causeString = path.lastEdge_! match {
    case _: ModelEdge => s"CONNECTIONFAILURETRIGGERPATH@${path.lastRelation_!.relationTableName}@${path.parentSideOfLastEdge}"
    case edge: NodeEdge =>
      s"CONNECTIONFAILURETRIGGERPATH@${path.lastRelation_!.relationTableName}@${path.parentSideOfLastEdge}@${path.childSideOfLastEdge}@${edge.childWhere.fieldValueAsString}}"
  }

  override val action = DatabaseMutationBuilder.connectionFailureTrigger(project, path, causeString)

  override val errorMapper = {
    case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodesNotConnectedError(path)
  }
}

case class VerifyWhereInterpreter(mutaction: VerifyWhere) extends DatabaseMutactionInterpreter {
  val project     = mutaction.project
  val where       = mutaction.where
  val causeString = s"WHEREFAILURETRIGGER@${where.model.name}@${where.field.name}@${where.fieldValueAsString}"

  override val action = DatabaseMutationBuilder.whereFailureTrigger(project, where, causeString)

  override val errorMapper = {
    case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodeNotFoundForWhereError(where)
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
