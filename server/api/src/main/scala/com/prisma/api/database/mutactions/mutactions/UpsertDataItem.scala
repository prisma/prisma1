package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException
import com.prisma.api.database.mutactions.GetFieldFromSQLUniqueException._
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.database.{DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.{CoolArgs, NodeSelector, SqlMutactions}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.json.JsonFormats

import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpsertDataItem(
    project: Project,
    where: NodeSelector,
    createWhere: NodeSelector,
    updateWhere: NodeSelector,
    allArgs: CoolArgs,
    dataResolver: DataResolver
) extends ClientSqlDataChangeMutaction {

  val model      = where.model
  val createArgs = allArgs.createArgumentsAsCoolArgs.generateNonListCreateArgs(model, createWhere.fieldValueAsString)
  val updateArgs = allArgs.updateArgumentsAsCoolArgs.generateNonListUpdateArgs(model)

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val createActions = SqlMutactions(dataResolver).getDbActionsForUpsertScalarLists(createWhere, allArgs.createArgumentsAsCoolArgs)
    val updateActions = SqlMutactions(dataResolver).getDbActionsForUpsertScalarLists(updateWhere, allArgs.updateArgumentsAsCoolArgs)
    Future.successful { ClientSqlStatementResult(DatabaseMutationBuilder.upsert(project.id, where, createArgs, updateArgs, createActions, updateActions)) }
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && getFieldOptionFromCoolArgs(List(createArgs, updateArgs), e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, getFieldOptionFromCoolArgs(List(createArgs, updateArgs), e).get)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 => APIErrors.NodeDoesNotExist(where.fieldValueAsString)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 => APIErrors.FieldCannotBeNull()
    })
  }
  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs.nonListScalarArguments(model).toList)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs.nonListScalarArguments(model).toList)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _)      => Future.successful(createCheck)
      case (_, true)      => Future.successful(updateCheck)
      case (false, false) => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
