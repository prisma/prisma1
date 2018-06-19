package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.postgresql.impl.GetFieldFromSQLUniqueException.getFieldOption
import com.prisma.api.schema.{APIErrors, UserFacingError}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models.{Field, Project, Relation, RelationField}
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

case class NestedCreateDataItemInterpreter(mutaction: NestedCreateDataItem, includeRelayRow: Boolean = true)(implicit ec: ExecutionContext)
    extends DatabaseMutactionInterpreter {
  val project  = mutaction.project
  val model    = mutaction.relationField.relatedModel_!
  val parent   = mutaction.relationField.model
  val relation = mutaction.relationField.relation

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
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

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

case class DeleteDataItemInterpreter(mutaction: DeleteDataItem)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
//Fixme Toplevel Mutations should not have a parentId
  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    for {
      _ <- mutationBuilder.deleteRelayRowJooq(mutaction.where)
      _ <- mutationBuilder.deleteDataItemJooq(mutaction.where)
    } yield UnitDatabaseMutactionResult
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

case class DeleteDataItemNestedInterpreter(mutaction: NestedDeleteDataItem)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    val parent         = mutaction.relationField.model
    val parentSelector = NodeSelector(parent, parent.idField_!, parentId)
    val path           = Path.empty(parentSelector).appendEdge(mutaction.relationField)
    for {
      _ <- mutationBuilder.deleteRelayRow(path)
      _ <- mutationBuilder.deleteDataItem(path)
    } yield UnitDatabaseMutactionResult
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
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getMessage).get)
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
    val requiredCheck = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.oldParentFailureTriggerByField(path, field, causeString(field)))
    DBIOAction.seq(requiredCheck: _*)
  }

  override val errorMapper = {
    case e: PSQLException if otherFailingRequiredRelationOnChild(e.getMessage).isDefined =>
      throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getMessage).get)
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

case class UpdateDataItemInterpreter(mutaction: UpdateDataItem) extends DatabaseMutactionInterpreter {
  val interpreter = SharedUpdateDataItemInterpreter(mutaction.project, Path.empty(mutaction.where), mutaction.nonListArgs, mutaction.listArgs)

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parent: IdGCValue)(implicit ec: ExecutionContext) = {
    interpreter.newAction(mutationBuilder, parent)
  }

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

  override val errorMapper = interpreter.errorMapper
}

case class NestedUpdateDataItemInterpreter(mutaction: NestedUpdateDataItem) extends DatabaseMutactionInterpreter {
  val model = mutaction.relationField.model

  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parent: IdGCValue)(implicit ec: ExecutionContext) = {
    val path = {
      val pathToParent = Path.empty(NodeSelector.forIdGCValue(model, parent))
      mutaction.where match {
        case Some(where) => pathToParent.append(NodeEdge(mutaction.relationField, where))
        case None        => pathToParent.append(ModelEdge(mutaction.relationField))
      }
    }
    val interpreter = SharedUpdateDataItemInterpreter(mutaction.project, path, mutaction.nonListArgs, mutaction.listArgs)
    interpreter.newAction(mutationBuilder, parent)
  }
  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???
}

case class SharedUpdateDataItemInterpreter(
    project: Project,
    path: Path,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)]
) extends DatabaseMutactionInterpreter {
  import scala.concurrent.ExecutionContext.Implicits.global

  val model = path.lastModel

  def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue) = {
    for {
      id <- mutationBuilder.pathQueryForLastChild(path).as[String].headOption
      _  <- mutationBuilder.updateDataItemByPath(path, nonListArgs)
      _  <- mutationBuilder.setScalarList(path.lastCreateWhere_!, listArgs)
    } yield UpdateItemResult(id.map(CuidGCValue))
  }

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = ???

  override val errorMapper = {
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(model, e).get)

    case e: PSQLException if e.getSQLState == "23503" =>
      APIErrors.NodeNotFoundForWhereError(path.root)

    case e: PSQLException if e.getSQLState == "23502" =>
      APIErrors.FieldCannotBeNull()
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
  val model      = mutaction.updatePath.lastModel
  val project    = mutaction.project
  val createArgs = mutaction.nonListCreateArgs
  val updateArgs = mutaction.nonListUpdateArgs

  def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val createNested: Vector[DBIOAction[Any, NoStream, Effect.All]] = mutaction.createMutactions.map(executor.interpreterFor).map(_.action(mutationBuilder))
    val updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]] = mutaction.updateMutactions.map(executor.interpreterFor).map(_.action(mutationBuilder))

    val createAction = mutationBuilder.setScalarList(mutaction.createPath.lastCreateWhere_!, mutaction.listCreateArgs)
    val updateAction = mutationBuilder.setScalarList(mutaction.updatePath.lastCreateWhere_!, mutaction.listUpdateArgs)
    mutationBuilder.upsert(
      createPath = mutaction.createPath,
      updatePath = mutaction.updatePath,
      createArgs = createArgs,
      updateArgs = updateArgs,
      create = createAction,
      update = updateAction,
      createNested = createNested,
      updateNested = updateNested
    )
  }

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

  override val errorMapper = { case e: PSQLException if e.getMessage.contains(causeString) => throw APIErrors.NodesNotConnectedError(path) }
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
