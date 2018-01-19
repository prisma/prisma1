package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.database.mutactions._
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.database.{DataResolver, DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import com.prisma.api.mutations.CoolArgs
import com.prisma.api.mutations.MutationTypes.{ArgumentValue, ArgumentValueList}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models._
import com.prisma.util.gc_value.GCValueExtractor
import com.prisma.util.json.JsonFormats
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

  val jsonCheckedValues: List[ArgumentValue] = { // we do not store the transformed version, why?
    if (model.fields.exists(_.typeIdentifier == TypeIdentifier.Json)) {
      InputValueValidation.transformStringifiedJson(values, model)
    } else {
      values
    }
  }

  def generateCoolArgsWithDefaultValues(model: Model, values: List[ArgumentValue]): CoolArgs = {
    val valuesWithDefault = model.scalarNonListFields.flatMap { field =>
      values.find(_.name == field.name) match {
        case Some(v) if v.value == None && field.defaultValue.isEmpty && field.isRequired => throw APIErrors.InputInvalid("null", field.name, model.name)
        case Some(v)                                                                      => Some((field.name, v.value))
        case None if field.defaultValue.isDefined                                         => Some((field.name, GCValueExtractor.fromGCValue(field.defaultValue.get)))
        case None                                                                         => None
      }
    }.toMap
    CoolArgs(valuesWithDefault)
  }

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.createDataItem(project.id, model.name, generateCoolArgsWithDefaultValues(model, values)),
          relayIds += ProjectRelayId(id = id, model.stableIdentifier)
        )))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      case e: SQLIntegrityConstraintViolationException
          if e.getErrorCode == 1062 && GetFieldFromSQLUniqueException.getFieldOptionFromArgumentValueList(jsonCheckedValues, e).isDefined =>
        APIErrors.UniqueConstraintViolation(model.name, GetFieldFromSQLUniqueException.getFieldOptionFromArgumentValueList(jsonCheckedValues, e).get)
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
