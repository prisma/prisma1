package com.prisma.api.connector.mysql.impl

import java.sql.{SQLException, SQLIntegrityConstraintViolationException}

import com.prisma.api.connector.mysql.DatabaseMutactionInterpreter
import com.prisma.api.connector._
import com.prisma.api.database.{DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import com.prisma.api.database.DatabaseMutationBuilder.{cascadingDeleteChildActions, oldParentFailureTriggerByField, oldParentFailureTriggerByFieldAndFilter}
import com.prisma.api.database.mutactions.{ClientSqlStatementResult, GetFieldFromSQLUniqueException}
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterString
import org.scalatest.path
import slick.dbio.DBIOAction
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class AddDataItemToManyRelationByPathInterpreter(mutaction: AddDataItemToManyRelationByPath) extends DatabaseMutactionInterpreter {

  override val action = DatabaseMutationBuilder.createRelationRowByPath(mutaction.project.id, mutaction.path)
}

case class CascadingDeleteRelationMutactionsInterpreter(mutaction: CascadingDeleteRelationMutactions) extends DatabaseMutactionInterpreter {
  val path    = mutaction.path
  val project = mutaction.project

  val fieldsWhereThisModelIsRequired = project.schema.allFields.filter { f =>
    f.isRequired && !f.isList && f.relatedModel(project.schema).contains(path.lastModel)
  }

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
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}

case class CreateDataItemInterpreter(mutaction: CreateDataItem) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path
  val args    = mutaction.args
  val model   = path.lastModel
  val where = path.edges match {
    case x if x.isEmpty => path.root
    case x              => x.last.asInstanceOf[NodeEdge].childWhere
  }
  val id = where.fieldValueAsString

  override val action = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))
    DBIO.seq(
      DatabaseMutationBuilder.createDataItem(project.id, model.name, args.generateNonListCreateArgs(model, id)),
      relayIds += ProjectRelayId(id = id, model.stableIdentifier)
    )
  }

  override val errorMapper = {
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOption(List(args), e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(List(args), e).get)
    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
      APIErrors.NodeDoesNotExist("")
  }
}

case class DeleteDataItemInterpreter(mutaction: DeleteDataItem) extends DatabaseMutactionInterpreter {
  override def action = DBIO.seq(
    DatabaseMutationBuilder.deleteRelayRow(mutaction.project.id, mutaction.path),
    DatabaseMutationBuilder.deleteDataItem(mutaction.project.id, mutaction.path)
  )
}

case class DeleteDataItemNestedInterpreter(mutaction: DeleteDataItemNested) extends DatabaseMutactionInterpreter {
  override def action = DBIO.seq(
    DatabaseMutationBuilder.deleteRelayRow(mutaction.project.id, mutaction.path),
    DatabaseMutationBuilder.deleteDataItem(mutaction.project.id, mutaction.path)
  )
}

case class DeleteDataItemsInterpreter(mutaction: DeleteDataItems) extends DatabaseMutactionInterpreter {
  val project     = mutaction.project
  val model       = mutaction.model
  val whereFilter = mutaction.whereFilter

  override def action = DBIOAction.seq(
    DatabaseMutationBuilder.deleteRelayIds(project, model, whereFilter),
    DatabaseMutationBuilder.deleteDataItems(project, model, whereFilter)
  )
}

case class DeleteManyRelationChecksInterpreter(mutaction: DeleteManyRelationChecks) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val model   = mutaction.model
  val filter  = mutaction.filter

  val fieldsWhereThisModelIsRequired = project.schema.allFields.filter { f =>
    f.isRequired && !f.isList && f.relatedModel(project.schema).contains(model)
  }

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
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER WHERE `${field.oppositeRelationSide.get}`"
    cause.contains(parentCheckString) //todo add filter
  }
}

case class DeleteRelationCheckInterpreter(mutaction: DeleteRelationCheck) extends DatabaseMutactionInterpreter {
  val project = mutaction.project
  val path    = mutaction.path

  val fieldsWhereThisModelIsRequired = project.schema.allFields.filter { f =>
    f.isRequired && !f.isList && f.relatedModel(project.schema).contains(path.lastModel)
  }

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
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}

object DisableForeignKeyConstraintChecksInterpreter extends DatabaseMutactionInterpreter {
  override def action = DatabaseMutationBuilder.disableForeignKeyConstraintChecks
}

object EnableForeignKeyConstraintChecksInterpreter extends DatabaseMutactionInterpreter {
  override def action = DatabaseMutationBuilder.enableForeignKeyConstraintChecks
}
