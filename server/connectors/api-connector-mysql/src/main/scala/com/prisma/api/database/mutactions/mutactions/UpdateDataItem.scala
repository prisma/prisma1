package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector.{DataItem, NodeSelector}
import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, GetFieldFromSQLUniqueException, MutactionVerificationSuccess}
import com.prisma.api.mutations.CoolArgs
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateDataItem(project: Project,
                          model: Model,
                          id: Id,
                          args: CoolArgs,
                          previousValues: DataItem,
                          requestId: Option[String] = None,
                          itemExists: Boolean)
    extends ClientSqlDataChangeMutaction {

  // TODO filter for fields which actually did change
  val namesOfUpdatedFields: Vector[String] = args.raw.keys.toVector

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(DatabaseMutationBuilder.updateDataItemByUnique(project.id, NodeSelector.forId(model, id), args)))

  }

  override def handleErrors = {
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOption(List(args), e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOption(List(args), e).get)

      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        APIErrors.NodeDoesNotExist(id)

      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
        APIErrors.FieldCannotBeNull()
    })
  }

  override def verify(): Try[MutactionVerificationSuccess] = {
    lazy val (dataItemInputValidation, fieldsWithValues) = InputValueValidation.validateDataItemInputs(model, args)

    def isReadonly(field: Field): Boolean = {
      // todo: replace with readOnly property on Field
      val isReadOnlyFileField = model.name == "File" && List("secret", "url", "contentType", "size").contains(field.name)
      field.isReadonly || isReadOnlyFileField
    }

    lazy val readonlyFields = fieldsWithValues.filter(isReadonly)

    itemExists match {
      case false                                  => Failure(APIErrors.DataItemDoesNotExist(model.name, id))
      case _ if dataItemInputValidation.isFailure => dataItemInputValidation
      case _ if readonlyFields.nonEmpty           => Failure(APIErrors.ReadonlyField(readonlyFields.mkString(",")))
      case _                                      => Success(MutactionVerificationSuccess())

    }
  }
}
