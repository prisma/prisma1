package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, GetFieldFromSQLUniqueException}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models._
import com.prisma.util.json.JsonFormats
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class SetScalarList(
    project: Project,
    where: NodeSelector,
    field: Field,
    values: Vector[Any]
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.setScalarList(project.id, where, field.name, values)
        )))
  }

//  override def handleErrors = {
//    implicit val anyFormat = JsonFormats.AnyJsonFormat
//    Some({
//      //https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
//      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
//        APIErrors.UniqueConstraintViolation(where.model.name, GetFieldFromSQLUniqueException.getFieldFromArgumentValueList(jsonCheckedValues, e))
//      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
//        APIErrors.NodeDoesNotExist("")
//    })
//  }
//
//  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
//    val (check, _) = InputValueValidation.validateDataItemInputsWithID(model, id, jsonCheckedValues)
//    if (check.isFailure) return Future.successful(check)
//
//    resolver.existsByModelAndId(model, id) map {
//      case true  => Failure(APIErrors.DataItemAlreadyExists(model.name, id))
//      case false => Success(MutactionVerificationSuccess())
//    }
//  }
}
