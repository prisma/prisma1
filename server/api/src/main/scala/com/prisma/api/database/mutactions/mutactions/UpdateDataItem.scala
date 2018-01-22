package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, GetFieldFromSQLUniqueException, MutactionVerificationSuccess}
import com.prisma.api.database.{DataItem, DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.MutationTypes.ArgumentValue
import com.prisma.api.mutations.{CoolArgs, NodeSelector}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project}
import com.prisma.util.json.JsonFormats

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateDataItem(project: Project,
                          model: Model,
                          id: Id,
                          values: Vector[ArgumentValue],
                          previousValues: DataItem,
                          requestId: Option[String] = None,
                          originalArgs: Option[CoolArgs] = None,
                          itemExists: Boolean)
    extends ClientSqlDataChangeMutaction {

  // TODO filter for fields which actually did change
  val namesOfUpdatedFields: Vector[String] = values.map(_.name)

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        DatabaseMutationBuilder.updateDataItemByUnique(project.id, NodeSelector.forId(model, id), CoolArgs(values.map(x => (x.name, x.value)).toMap))))

  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException
          if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOptionFromArgumentValueList(values.toList, e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionFromArgumentValueList(values.toList, e).get)

      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        APIErrors.NodeDoesNotExist(id)

      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
        APIErrors.FieldCannotBeNull()
    })
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    lazy val (dataItemInputValidation, fieldsWithValues) = InputValueValidation.validateDataItemInputsWithID(model, id, values.toList)

    def isReadonly(field: Field): Boolean = {
      // todo: replace with readOnly property on Field
      val isReadOnlyFileField = model.name == "File" && List("secret", "url", "contentType", "size").contains(field.name)
      field.isReadonly || isReadOnlyFileField
    }

    lazy val readonlyFields = fieldsWithValues.filter(isReadonly)

    val checkResult = itemExists match {
      case false                                  => Failure(APIErrors.DataItemDoesNotExist(model.name, id))
      case _ if dataItemInputValidation.isFailure => dataItemInputValidation
      case _ if readonlyFields.nonEmpty           => Failure(APIErrors.ReadonlyField(readonlyFields.mkString(",")))
      case _                                      => Success(MutactionVerificationSuccess())

    }
    Future.successful(checkResult)
  }
}
