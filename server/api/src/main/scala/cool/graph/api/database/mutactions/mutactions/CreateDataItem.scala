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

case class CreateDataItem(
    project: Project,
    model: Model,
    values: List[ArgumentValue],
    originalArgs: Option[CoolArgs] = None
) extends ClientSqlDataChangeMutaction {

  // FIXME: it should be guaranteed to always have an id (generate it in here)
  val id: Id = ArgumentValueList.getId_!(values)

  val jsonCheckedValues: List[ArgumentValue] = {
    if (model.fields.exists(_.typeIdentifier == TypeIdentifier.Json)) {
      InputValueValidation.transformStringifiedJson(values, model)
    } else {
      values
    }
  }

  def getValueOrDefault(transformedValues: List[ArgumentValue], field: Field): Option[Any] = {
    transformedValues
      .find(_.name == field.name)
      .map(v => Some(v.value))
      .getOrElse(field.defaultValue.map(GCDBValueConverter().fromGCValue))
  }

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds          = TableQuery(new ProjectRelayIdTable(_, project.id))
    val valuesIncludingId = jsonCheckedValues :+ ArgumentValue("id", id)

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.createDataItem(
            project.id,
            model.name,
            model.scalarFields
              .filter(!_.isList)
              .filter(getValueOrDefault(values, _).isDefined)
              .map(field => (field.name, getValueOrDefault(values, field).get))
              .toMap
          ),
          relayIds += ProjectRelayId(id = ArgumentValueList.getId_!(jsonCheckedValues), model.id)
        )))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      //https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldFromArgumentValueList(jsonCheckedValues, e))
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        APIErrors.NodeDoesNotExist("")
    })
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (check, _) = InputValueValidation.validateDataItemInputsWithID(model, id, jsonCheckedValues)
    if (check.isFailure) return Future.successful(check)

    resolver.existsByModelAndId(model, id) map {
      case true  => Failure(APIErrors.DataItemAlreadyExists(model.name, id))
      case false => Success(MutactionVerificationSuccess())
    }
  }
}
