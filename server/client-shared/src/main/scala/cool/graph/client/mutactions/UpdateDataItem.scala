package cool.graph.client.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.client.database.GetFieldFromSQLUniqueException.getField
import cool.graph.client.mutactions.validation.InputValueValidation
import cool.graph.client.mutations.CoolArgs
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Model, Project, RequestPipelineOperation}
import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateDataItem(project: Project,
                          model: Model,
                          id: Id,
                          values: List[ArgumentValue],
                          previousValues: DataItem,
                          requestId: Option[String] = None,
                          originalArgs: Option[CoolArgs] = None,
                          itemExists: Boolean)(implicit val inj: Injector)
    extends ClientSqlMutaction {

  val pipelineRunner = new RequestPipelineRunner(requestId.getOrElse(""))

  // TODO filter for fields which actually did change
  val namesOfUpdatedFields: List[String] = values.map(_.name)

  private def getFieldMirrors = {
    val mirrors = model.fields
      .flatMap(_.relation)
      .flatMap(_.fieldMirrors)
      .filter(mirror => model.fields.map(_.id).contains(mirror.fieldId))

    mirrors
  }

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val mirrorUpdates = getFieldMirrors.flatMap(mirror => {
      val relation = project.getRelationById_!(mirror.relationId)
      val field    = project.getFieldById_!(mirror.fieldId)

      values.find(_.name == field.name).map(_.value) match {
        case Some(value) =>
          List(
            DatabaseMutationBuilder.updateRelationRow(
              project.id,
              mirror.relationId,
              relation.fieldSide(project, field).toString,
              id,
              Map(RelationFieldMirrorColumn.mirrorColumnName(project, field, relation) -> value)
            ))
        case None => List()
      }

    })

    val valuesIncludingId = values :+ ArgumentValue("id", id, model.getFieldByName_!("id"))
    for {
      transformedValues <- pipelineRunner
                            .runTransformArgument(project, model, RequestPipelineOperation.UPDATE, valuesIncludingId, originalArgs)
      _ <- pipelineRunner.runPreWrite(project, model, RequestPipelineOperation.UPDATE, transformedValues, originalArgs)
    } yield {
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          List(
            DatabaseMutationBuilder
              .updateDataItem(project.id,
                              model.name,
                              id,
                              transformedValues
                                .map(x => (x.name, x.value))
                                .toMap)) ++ mirrorUpdates: _*))
    }
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat

    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        UserAPIErrors.UniqueConstraintViolation(model.name, getField(values, e))
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        UserAPIErrors.NodeDoesNotExist(id)
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 =>
        UserAPIErrors.FieldCannotBeNull()
    })
  }

  override def verify: Future[Try[MutactionVerificationSuccess]] = {
    lazy val (dataItemInputValidation, fieldsWithValues) = InputValueValidation.validateDataItemInputs(model, id, values)

    def isReadonly(field: Field): Boolean = {
      // todo: replace with readOnly property on Field
      val isReadOnlyFileField = model.name == "File" && List("secret", "url", "contentType", "size").contains(field.name)
      field.isReadonly || isReadOnlyFileField
    }

    lazy val readonlyFields = fieldsWithValues.filter(isReadonly)

    val checkResult = itemExists match {
      case false                                  => Failure(UserAPIErrors.DataItemDoesNotExist(model.name, id))
      case _ if dataItemInputValidation.isFailure => dataItemInputValidation
      case _ if readonlyFields.nonEmpty           => Failure(UserAPIErrors.ReadonlyField(readonlyFields.mkString(",")))
      case _                                      => Success(MutactionVerificationSuccess())

    }
    Future.successful(checkResult)
  }
}
