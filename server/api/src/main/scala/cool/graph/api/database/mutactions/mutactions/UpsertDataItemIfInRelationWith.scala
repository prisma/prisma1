package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.database.mutactions.GetFieldFromSQLUniqueException._
import cool.graph.api.database.mutactions.validation.InputValueValidation
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.api.schema.APIErrors
import cool.graph.cuid.Cuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project}
import cool.graph.util.gc_value.GCStringConverter
import cool.graph.util.json.JsonFormats

import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpsertDataItemIfInRelationWith(
    project: Project,
    fromField: Field,
    fromId: Id,
    createArgs: CoolArgs,
    updateArgs: CoolArgs,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {

  val model            = where.model
  val idOfNewItem      = Cuid.createCuid()
  val actualCreateArgs = CoolArgs(createArgs.raw + ("id" -> idOfNewItem))

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(
      DatabaseMutationBuilder.upsertIfInRelationWith(
        project = project,
        model = model,
        createArgs = actualCreateArgs,
        updateArgs = updateArgs,
        where = where,
        relation = fromField.relation.get,
        target = fromId
      ))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    val whereField         = model.fields.find(_.name == where.fieldName).get
    val converter          = GCStringConverter(whereField.typeIdentifier, whereField.isList)

    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        APIErrors.UniqueConstraintViolation(model.name, getFieldFromCoolArgs(List(createArgs, updateArgs), e))
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 => APIErrors.NodeDoesNotExist(converter.fromGCValue(where.fieldValue))
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 => APIErrors.FieldCannotBeNull()
    })
  }
  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs.scalarArguments(model).toList)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs.scalarArguments(model).toList)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _)      => Future.successful(createCheck)
      case (_, true)      => Future.successful(updateCheck)
      case (false, false) => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
