package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.mutations.mutations.CascadingDeletes.{NodeEdge, Path}
import com.prisma.shared.models._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class SetScalarList(
    project: Project,
    path: Path,
    field: Field,
    values: Vector[Any]
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.setScalarListPath(project.id, path, field.name, values)
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
