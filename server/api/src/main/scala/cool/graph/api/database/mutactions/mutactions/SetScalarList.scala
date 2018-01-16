package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.database.mutactions.validation.InputValueValidation
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, GetFieldFromSQLUniqueException, MutactionVerificationSuccess}
import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import cool.graph.api.mutations.CoolArgs
import cool.graph.api.mutations.MutationTypes.{ArgumentValue, ArgumentValueList}
import cool.graph.api.schema.APIErrors
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._
import cool.graph.util.gc_value.GCDBValueConverter
import cool.graph.util.json.JsonFormats
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class SetScalarList(
    project: Project,
    model: Model,
    field: Field,
    values: Vector[Any],
    nodeId: String
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.setScalarList(project.id, model.name, field.name, nodeId, values)
        )))
  }

//  override def handleErrors = {
//    implicit val anyFormat = JsonFormats.AnyJsonFormat
//    Some({
//      //https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
//      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
//        APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldFromArgumentValueList(jsonCheckedValues, e))
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
