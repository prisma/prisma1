package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector.{NodeSelector, Path}
import com.prisma.api.database.mutactions.GetFieldFromSQLUniqueException._
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.database.{DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.{CoolArgs, SqlMutactions}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.json.JsonFormats

import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpsertDataItem(
    project: Project,
    path: Path,
    createWhere: NodeSelector,
    updatedWhere: NodeSelector,
    allArgs: CoolArgs,
    dataResolver: DataResolver
) extends ClientSqlDataChangeMutaction {

  val model      = path.lastModel
  val createArgs = allArgs.createArgumentsAsCoolArgs.generateNonListCreateArgs(model, createWhere.fieldValueAsString)
  val updateArgs = allArgs.updateArgumentsAsCoolArgs.generateNonListUpdateArgs(model)

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val createActions = SqlMutactions(dataResolver).getDbActionsForUpsertScalarLists(path.updatedRoot(createArgs), allArgs.createArgumentsAsCoolArgs)
    val updateActions = SqlMutactions(dataResolver).getDbActionsForUpsertScalarLists(path.updatedRoot(updateArgs), allArgs.updateArgumentsAsCoolArgs)
    Future.successful(
      ClientSqlStatementResult(DatabaseMutationBuilder.upsert(project.id, path, createWhere, createArgs, updateArgs, createActions, updateActions)))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && getFieldOption(List(createArgs, updateArgs), e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, getFieldOption(List(createArgs, updateArgs), e).get)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 => APIErrors.NodeDoesNotExist("") //todo
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 => APIErrors.FieldCannotBeNull(e.getCause.getMessage)
    })
  }

  override def verify(): Try[MutactionVerificationSuccess] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _)      => createCheck
      case (_, true)      => updateCheck
      case (false, false) => Success(MutactionVerificationSuccess())
    }
  }
}
