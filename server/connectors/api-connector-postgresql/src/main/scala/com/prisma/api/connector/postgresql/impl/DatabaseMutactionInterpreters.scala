package com.prisma.api.connector.postgresql.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostGresApiDatabaseMutationBuilder
import com.prisma.api.connector.postgresql.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Relation}
import org.postgresql.util.PSQLException
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

case class AddDataItemToManyRelationByPathInterpreter(mutaction: AddDataItemToManyRelationByPath) extends DatabaseMutactionInterpreter {

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.createRelationRowByPath(mutaction.path)
}

case class CascadingDeleteRelationMutactionsInterpreter(mutaction: CascadingDeleteRelationMutactions) extends DatabaseMutactionInterpreter {
  val path    = mutaction.path
  val project = mutaction.project

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(path.lastModel)

  val otherFieldsWhereThisModelIsRequired = path.lastEdge match {
    case Some(edge) => fieldsWhereThisModelIsRequired.filter(f => f != edge.parentField)
    case None       => fieldsWhereThisModelIsRequired
  }

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val requiredCheck = otherFieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(path, field, causeString(field)))
    val deleteAction  = List(mutationBuilder.cascadingDeleteChildActions(path))
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

case class CreateDataItemInterpreter(mutaction: CreateDataItem, includeRelayRow: Boolean = true) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  import scala.concurrent.ExecutionContext.Implicits.global

  override def newAction(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val unitAction    = DBIO.successful(UnitDatabaseMutactionResult)
    val createNonList = mutationBuilder.createDataItem(path, mutaction.nonListArgs)
    val listAction    = mutationBuilder.setScalarList(path, mutaction.listArgs).andThen(unitAction)

    if (includeRelayRow) {
      val createRelayRow = mutationBuilder.createRelayRow(path).andThen(unitAction)
      DBIO.sequence(Vector(createNonList, createRelayRow, listAction)).map(_.head)
    } else {
      DBIO.sequence(Vector(createNonList, listAction)).map(_.head)
    }
  }

  override def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = ???

  override val errorMapper = {
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).get)
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("")
  }
}

case class DeleteDataItemInterpreter(mutaction: DeleteDataItem) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = DBIO.seq(
    mutationBuilder.deleteRelayRow(mutaction.path),
    mutationBuilder.deleteDataItem(mutaction.path)
  )
}

case class DeleteDataItemNestedInterpreter(mutaction: DeleteDataItemNested) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = DBIO.seq(
    mutationBuilder.deleteRelayRow(mutaction.path),
    mutationBuilder.deleteDataItem(mutaction.path)
  )
}

case class DeleteDataItemsInterpreter(mutaction: DeleteDataItems) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = DBIOAction.seq(
    mutationBuilder.deleteRelayIds(mutaction.model, mutaction.whereFilter),
    mutationBuilder.deleteDataItems(mutaction.model, mutaction.whereFilter)
  )
}

case class DeleteManyRelationChecksInterpreter(mutaction: DeleteManyRelationChecks) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val model   = mutaction.model
  val filter  = mutaction.whereFilter

  val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val requiredChecks =
      fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByFieldAndFilter(model, filter, field, causeString(field)))
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

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val requiredCheck = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(path, field, causeString(field)))
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
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val truncateTables = DBIOAction.sequence(mutaction.tableNames.map(mutationBuilder.truncateTable))
    DBIOAction.seq(truncateTables)
  }
}

case class UpdateDataItemInterpreter(mutaction: UpdateWrapper) extends DatabaseMutactionInterpreter {
  val (project, path, nonListArgs, listArgs) = mutaction match {
    case x: UpdateDataItem       => (x.project, x.path, x.nonListArgs, x.listArgs)
    case x: NestedUpdateDataItem => (x.project, x.path, x.nonListArgs, x.listArgs)
  }

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val nonListAction = mutationBuilder.updateDataItemByPath(path, nonListArgs)
    val listAction    = mutationBuilder.setScalarList(path, listArgs)
    DBIO.seq(listAction, nonListAction)
  }

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
  //update Lists before updating the nodes
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val nonListActions = mutationBuilder.updateDataItems(mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
    val listActions    = mutationBuilder.setManyScalarLists(mutaction.model, mutaction.listArgs, mutaction.whereFilter)
    DBIOAction.seq(listActions, nonListActions)
  }
}

case class UpsertDataItemInterpreter(mutaction: UpsertDataItem) extends DatabaseMutactionInterpreter {
  val model      = mutaction.updatePath.lastModel
  val project    = mutaction.project
  val createArgs = mutaction.nonListCreateArgs
  val updateArgs = mutaction.nonListUpdateArgs

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val createAction = mutationBuilder.setScalarList(mutaction.createPath, mutaction.listCreateArgs)
    val updateAction = mutationBuilder.setScalarList(mutaction.updatePath, mutaction.listUpdateArgs)
    mutationBuilder.upsert(mutaction.createPath, mutaction.updatePath, createArgs, updateArgs, createAction, updateAction)
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

  val relationChecker = NestedCreateRelationInterpreter(NestedCreateRelation(project, mutaction.createPath, false))

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = {
    val createCheck       = DBIOAction.seq(relationChecker.allActions(mutationBuilder): _*)
    val scalarListsCreate = mutationBuilder.setScalarList(mutaction.createPath, mutaction.createListArgs)
    val scalarListsUpdate = mutationBuilder.setScalarList(mutaction.updatePath, mutaction.updateListArgs)
    mutationBuilder.upsertIfInRelationWith(
      createPath = mutaction.createPath,
      updatePath = mutaction.updatePath,
      createArgs = mutaction.createNonListArgs,
      updateArgs = mutaction.updateNonListArgs,
      scalarListCreate = scalarListsCreate,
      scalarListUpdate = scalarListsUpdate,
      createCheck = createCheck
    )
  }

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

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.connectionFailureTrigger(path, causeString)

  override val errorMapper = {
    case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodesNotConnectedError(path)
  }
}

case class VerifyWhereInterpreter(mutaction: VerifyWhere) extends DatabaseMutactionInterpreter {
  val project     = mutaction.project
  val where       = mutaction.where
  val causeString = s"WHEREFAILURETRIGGER@${where.model.name}@${where.field.name}@${where.fieldValueAsString}"

  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.whereFailureTrigger(where, causeString)

  override val errorMapper = {
    case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodeNotFoundForWhereError(where)
  }
}

case class CreateDataItemsImportInterpreter(mutaction: CreateDataItemsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.createDataItemsImport(mutaction)
}

case class CreateRelationRowsImportInterpreter(mutaction: CreateRelationRowsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.createRelationRowsImport(mutaction)
}

case class PushScalarListsImportInterpreter(mutaction: PushScalarListsImport) extends DatabaseMutactionInterpreter {
  def action(mutationBuilder: PostGresApiDatabaseMutationBuilder) = mutationBuilder.pushScalarListsImport(mutaction)
}
