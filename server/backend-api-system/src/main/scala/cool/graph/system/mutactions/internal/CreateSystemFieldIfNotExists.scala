package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Field, Model, Project}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.{FieldTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Allows for insertion of system fields with minimal validation checks.
  * Usually you want to use CreateField.
  */
case class CreateSystemFieldIfNotExists(
    project: Project,
    model: Model,
    field: Field
) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val fields   = TableQuery[FieldTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          fields += ModelToDbMapper.convertField(model.id, field),
          relayIds += cool.graph.system.database.tables.RelayId(field.id, "Field")
        )))
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] =
    Some(DeleteField(project, model, field, allowDeleteSystemField = true).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val verifyResult = if (model.fields.exists(_.name.toLowerCase == field.name.toLowerCase)) {
      Failure(UserInputErrors.FieldAreadyExists(field.name))
    } else {
      Success(MutactionVerificationSuccess())
    }

    Future.successful(verifyResult)
  }
}
