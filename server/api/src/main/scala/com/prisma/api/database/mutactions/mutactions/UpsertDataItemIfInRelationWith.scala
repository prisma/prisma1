package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.database.mutactions.GetFieldFromSQLUniqueException._
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.database.{DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors
import cool.graph.cuid.Cuid
import com.prisma.shared.models.Project
import com.prisma.util.json.JsonFormats
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpsertDataItemIfInRelationWith(project: Project,
                                          parentInfo: ParentInfo,
                                          where: NodeSelector,
                                          createWhere: NodeSelector,
                                          createArgs: CoolArgs,
                                          updateArgs: CoolArgs,
                                          createMutations: Vector[DBIOAction[Any, NoStream, Effect]],
                                          updateMutations: Vector[DBIOAction[Any, NoStream, Effect]])
    extends ClientSqlDataChangeMutaction {

  val model            = where.model
  val actualCreateArgs = CoolArgs(createArgs.raw).generateNonListCreateArgs(model, createWhere.fieldValueAsString)
  val actualUpdateArgs = updateArgs.nonListScalarArguments(model)

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(
      DatabaseMutationBuilder
        .upsertIfInRelationWith(project, parentInfo, where, createWhere, actualCreateArgs, actualUpdateArgs, createMutations, updateMutations))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && getFieldOption(List(createArgs, updateArgs), e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, getFieldOption(List(createArgs, updateArgs), e).get)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 => APIErrors.NodeDoesNotExist(where.fieldValueAsString)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 => APIErrors.FieldCannotBeNull()
    })
  }
  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _)      => Future.successful(createCheck)
      case (_, true)      => Future.successful(updateCheck)
      case (false, false) => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
