package cool.graph.client.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.GCDataTypes._
import cool.graph.Types.Id
import cool.graph.client.database.GetFieldFromSQLUniqueException.getField
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import cool.graph.client.mutactions.validation.InputValueValidation.{transformStringifiedJson, validateDataItemInputs}
import cool.graph.client.mutations.CoolArgs
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models._
import cool.graph.shared.mutactions.MutationTypes.{ArgumentValue, ArgumentValueList}
import cool.graph.{ClientSqlStatementResult, MutactionVerificationSuccess, _}
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateDataItem(
    project: Project,
    model: Model,
    values: List[ArgumentValue],
    allowSettingManagedFields: Boolean = false,
    requestId: Option[String] = None,
    originalArgs: Option[CoolArgs] = None
)(implicit val inj: Injector)
    extends ClientSqlDataChangeMutaction
    with Injectable {

  val pipelineRunner = new RequestPipelineRunner(requestId.getOrElse(""))

  // FIXME: it should be guaranteed to always have an id (generate it in here)
  val id: Id = ArgumentValueList.getId_!(values)

  val jsonCheckedValues: List[ArgumentValue] = {
    if (model.fields.exists(_.typeIdentifier == TypeIdentifier.Json)) {
      transformStringifiedJson(values, model)
    } else {
      values
    }
  }

  def getValueOrDefault(transformedValues: List[ArgumentValue], field: Field): Option[Any] = {
    transformedValues
      .find(_.name == field.name)
      .map(v => Some(v.value))
      .getOrElse(field.defaultValue.map(GCDBValueConverter(field.typeIdentifier, field.isList).fromGCValue))
  }

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds          = TableQuery(new ProjectRelayIdTable(_, project.id))
    val valuesIncludingId = jsonCheckedValues :+ ArgumentValue("id", id, model.getFieldByName_!("id"))

    for {
      transformedValues <- pipelineRunner.runTransformArgument(project, model, RequestPipelineOperation.CREATE, valuesIncludingId, originalArgs)
      _                 <- pipelineRunner.runPreWrite(project, model, RequestPipelineOperation.CREATE, transformedValues, originalArgs)
    } yield {

      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.createDataItem(
            project.id,
            model.name,
            model.scalarFields
              .filter(getValueOrDefault(transformedValues, _).isDefined)
              .map(field => (field.name, getValueOrDefault(transformedValues, field).get))
              .toMap
          ),
          relayIds += ProjectRelayId(id = ArgumentValueList.getId_!(jsonCheckedValues), model.id)
        ))
    }
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      //https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        UserAPIErrors.UniqueConstraintViolation(model.name, getField(jsonCheckedValues, e))
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        UserAPIErrors.NodeDoesNotExist("")
    })
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (check, _) = validateDataItemInputs(model, id, jsonCheckedValues)
    if (check.isFailure) return Future.successful(check)

    resolver.existsByModelAndId(model, id) map {
      case true  => Failure(UserAPIErrors.DataItemAlreadyExists(model.name, id))
      case false => Success(MutactionVerificationSuccess())
    }
  }
}
